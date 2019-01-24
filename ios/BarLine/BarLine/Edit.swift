//
//  Edit.swift
//  BarLine
//
//  Created by Mike Yaworski on 2016-07-10.
//  Copyright © 2016 Mike Yaworski. All rights reserved.
//

import UIKit
import SwiftyJSON
import Alamofire
import Toast_Swift
import NVActivityIndicatorView
import KDCircularProgress
import Crashlytics

class Edit: UIViewController {
    @IBOutlet weak var lblBarName: UILabel!
    
    @IBOutlet weak var scrollView: UIScrollView!
    
    @IBOutlet weak var progressBarsView: UIView!
    
    @IBOutlet weak var crowdingLevelProgressView: UIView!
    @IBOutlet weak var waitTimeProgressView: UIView!
    @IBOutlet weak var lblWaitTime: UILabel!
    @IBOutlet weak var lblCrowdingLevel: UILabel!
    
    @IBOutlet weak var waitTimeLoadingAnimation: NVActivityIndicatorView!
    @IBOutlet weak var crowdingLevelLoadingAnimation: NVActivityIndicatorView!
    
    @IBOutlet weak var crowdingLevelProgressSlider: UISlider!
    @IBOutlet weak var waitTimeProgressSlider: UISlider!
    
    @IBOutlet weak var lblLastUpdated: UILabel!
    
    @IBOutlet weak var btnSave: UIButton!
    @IBAction func btnSaveReleased(sender: AnyObject) {
        btnSave.backgroundColor = UIColor.clearColor()
        
        let deviceName: String = UIDevice.currentDevice().name
        
        self.savingLoadingAnimation.startAnimation()
        Alamofire.request(.PUT, ApiVariables.waitTimeUrl + "/" + String(self.barId), headers: getAuthHeader(String(barId), password: password), parameters: ["waitTime": waitTimeProgressSlider.value * 5, "crowdingLevel": crowdingLevelProgressSlider.value * 5, "deviceName": deviceName], encoding: .JSON).responseJSON{ (response)->Void in
            
            switch response.result {
            case .Success:
                if let value = response.result.value {
                    let json = JSON(value)
                    
                    if let statusCode = response.response?.statusCode {
                        if statusCode >= 400 {
                            self.view.makeToast(String(statusCode) + " - " + json["error"].stringValue)
                            self.savingLoadingAnimation.stopAnimation()
                            return
                        }
                    }
                    
                    let successStatus = json["success_status"].intValue;
                    
                    if (successStatus == 1) {
                        self.navigationController?.popViewControllerAnimated(true)
                        self.waitTimeView.makeToast(json["message"].stringValue) // toast to WaitTime view so that the toast can be seen when this view is popped off
                    }
                    
                    self.savingLoadingAnimation.stopAnimation()
                }
            case .Failure(let error):
                self.view.makeToast("Error: " + error.localizedDescription)
            }
        }
    }
    @IBAction func btnSavePressed(sender: AnyObject) {
        btnSave.backgroundColor = UIColor().colorAccent()
    }
    @IBAction func btnSaveReleasedOutside(sender: AnyObject) {
        btnSave.backgroundColor = UIColor.clearColor()
    }
    
    @IBOutlet weak var savingLoadingAnimation: NVActivityIndicatorView!
    
    let waitTimeProgressBar = KDCircularProgress(frame: CGRect(x: 0, y: 0, width: 150, height: 150))
    let crowdingLevelProgressBar = KDCircularProgress(frame: CGRect(x: 0, y: 0, width: 150, height: 150))
    
    var barName = String()
    var barId = Int()
    var password = String()
    
    var waitTimeView = UIView() // for displaying toast to wait time view once this view is popped off
    
    var waitTime = Int()
    var crowdingLevel = Int()

    func refreshProgress() {
        self.waitTimeLoadingAnimation.startAnimation()
        self.crowdingLevelLoadingAnimation.startAnimation()
        
        waitTimeProgressBar.angle = 0
        crowdingLevelProgressBar.angle = 0
        lblWaitTime.text = ""
        lblCrowdingLevel.text = ""
        
        // commented out because I think it was causing the API calls to sometimes never load
        // attempt to delete API call cache
        /*let manager: Manager = {
            let configuration = NSURLSessionConfiguration.defaultSessionConfiguration()
            configuration.URLCache = nil
            return Manager(configuration: configuration)
        }() // doesn't seem to work
        NSURLCache.sharedURLCache().removeAllCachedResponses() // seems to work*/
        
        Alamofire.request(.GET, ApiVariables.waitTimeUrl + "/" + String(self.barId), parameters: [:]).responseJSON{ (response)->Void in
            
            switch response.result {
            case .Success:
                if let value = response.result.value {
                    let json = JSON(value)
                    
                    if let statusCode = response.response?.statusCode {
                        if statusCode >= 400 {
                            self.view.makeToast(String(statusCode) + " - " + json["error"].stringValue)
                            self.waitTimeLoadingAnimation.stopAnimation()
                            self.crowdingLevelLoadingAnimation.stopAnimation()
                            return
                        }
                    }
                    
                    let jsonObject = json;
                    
                    self.waitTime = jsonObject["wait_time"].intValue
                    self.crowdingLevel = jsonObject["crowding_level"].intValue
                    
                    self.waitTimeProgressSlider.value = Float(self.waitTime / 5);
                    self.crowdingLevelProgressSlider.value = Float(self.crowdingLevel / 5);
                    
                    self.populateProgressBars()
                    
                    // -0700 since it is stored as PST in server
                    let lastUpdatedFromAPI: String = jsonObject["last_updated"].stringValue + " -0700"
                    let dateFormatter = NSDateFormatter()
                    dateFormatter.dateFormat = "yyyy-MM-dd HH:mm:ss Z"
                    let date = dateFormatter.dateFromString(lastUpdatedFromAPI)
                    self.lblLastUpdated.text = "Last updated: " + getLastUpdatedStr(date!)
                    
                    self.waitTimeLoadingAnimation.stopAnimation()
                    self.crowdingLevelLoadingAnimation.stopAnimation()
                } else {
                    self.view.makeToast(ErrorMessages.CANT_GET_RESPONSE)
                }
            case .Failure(let error):
                self.view.makeToast("Error: " + error.localizedDescription)
                self.waitTimeLoadingAnimation.stopAnimation()
                self.crowdingLevelLoadingAnimation.stopAnimation()
            }
        }
        
    }
    
    //var animatingWaitTimer: NSTimer!;
    //var animatingCrowdTimer: NSTimer!;
    
    func populateProgressBars() {
        var waitTimeStr = "\(self.waitTime) mins"
        if self.waitTime == 60 {
            waitTimeStr = "60+ mins"
        } else if (self.waitTime == 0) {
            waitTimeStr = "No wait!"
        }
        self.lblWaitTime.text = waitTimeStr
        
        var percentage = Double(self.waitTime) / 60.0
        var angle = percentage * 360
        
        if (percentage <= Double(1) / Double(3)) {
            self.waitTimeProgressBar.setColors(UIColor().progressGreen())
        } else if (percentage <= Double(2) / Double(3)) {
            self.waitTimeProgressBar.setColors(UIColor().progressYellow())
        } else if (percentage <= Double(3) / Double(3)) {
            self.waitTimeProgressBar.setColors(UIColor().progressRed())
        }
        
        /*if self.animatingWaitTimer != nil && self.animatingWaitTimer.valid {
            self.animatingWaitTimer.invalidate()
            self.animatingWaitTimer = nil
            print("invalidating")
        }
        
        // timer to change colour of the progress bar (only while animating)
        self.animatingWaitTimer = NSTimer.scheduledTimerWithTimeInterval(
            0.1,
            target: self,
            selector: #selector(updateProgressColours),
            userInfo: nil,
            repeats: true
        )*/
        
        self.waitTimeProgressBar.animateToAngle(angle, duration: 1, completion: { (Bool) in
            /*self.updateProgressColours()
            self.animatingWaitTimer.invalidate()
            self.animatingWaitTimer = nil*/
        })
        
        var crowdingLevelStr = "\(self.crowdingLevel)%"
        if self.crowdingLevel >= 100 {
            crowdingLevelStr = "Full"
        } else if (self.crowdingLevel <= 0) {
            crowdingLevelStr = "Empty"
        }
        self.lblCrowdingLevel.text = crowdingLevelStr
        
        percentage = Double(self.crowdingLevel) / 100.0
        angle = percentage * 360
        
        if (percentage <= Double(1) / Double(3)) {
            self.crowdingLevelProgressBar.setColors(UIColor().progressGreen())
        } else if (percentage <= Double(2) / Double(3)) {
            self.crowdingLevelProgressBar.setColors(UIColor().progressYellow())
        } else if (percentage <= Double(3) / Double(3)) {
            self.crowdingLevelProgressBar.setColors(UIColor().progressRed())
         }
        
        /*if self.animatingCrowdTimer != nil && self.animatingCrowdTimer.valid {
            self.animatingCrowdTimer.invalidate()
            self.animatingCrowdTimer = nil
            print("invalidating")
        }
 
        // timer to change colour of the progress bar (only while animating)
        self.animatingCrowdTimer = NSTimer.scheduledTimerWithTimeInterval(
            0.1,
            target: self,
            selector: #selector(updateProgressColours),
            userInfo: nil,
            repeats: true
        )*/
        
        self.crowdingLevelProgressBar.animateToAngle(angle, duration: 1, completion: { (Bool) in
            /*self.updateProgressColours()
            self.animatingCrowdTimer.invalidate()
            self.animatingCrowdTimer = nil*/
        })
        
    }
    
    // currently, this function is called at the appropriate times
    // but I have no way of getting the value of the progressBar while it's animating
    // (.angle returns the value that it is animating to, no matter what point it's at in the animation)
    // so I've asked the creator to implement a feature where I can get the value of the angle while it's animating
    func updateProgressColours() {
        var percentage = waitTimeProgressBar.angle / 360.0;
        
        if (percentage <= Double(1) / Double(3)) {
            self.waitTimeProgressBar.setColors(UIColor().progressGreen())
        } else if (percentage <= Double(2) / Double(3)) {
            self.waitTimeProgressBar.setColors(UIColor().progressYellow())
        } else if (percentage <= Double(3) / Double(3)) {
            self.waitTimeProgressBar.setColors(UIColor().progressRed())
        }
        
        percentage = crowdingLevelProgressBar.angle / 360.0;
        
        if (percentage <= Double(1) / Double(3)) {
            self.crowdingLevelProgressBar.setColors(UIColor().progressGreen())
        } else if (percentage <= Double(2) / Double(3)) {
            self.crowdingLevelProgressBar.setColors(UIColor().progressYellow())
        } else if (percentage <= Double(3) / Double(3)) {
            self.crowdingLevelProgressBar.setColors(UIColor().progressRed())
        }
    }
    
    let step: Float = 1
    @IBAction func waitTimeProgressSliderChanged(sender: AnyObject) {
        let roundedValue = round(waitTimeProgressSlider.value / step) * step
        waitTimeProgressSlider.value = roundedValue
        
        let value = waitTimeProgressSlider.value
        let mins = Int(value * 5)
        
        let justChangedOnStep = self.waitTime != mins
        
        self.waitTime = mins
        if (justChangedOnStep) {
            populateProgressBars()
        }
    }
    @IBAction func crowdingLevelProgressSliderChanged(sender: AnyObject) {
        let roundedValue = round(crowdingLevelProgressSlider.value / step) * step
        crowdingLevelProgressSlider.value = roundedValue
        
        let value = crowdingLevelProgressSlider.value
        let percent = Int(value * 5)
        
        self.crowdingLevel = percent
        populateProgressBars()
    }

    override func viewDidLoad() {
        super.viewDidLoad()
        
        btnSave.backgroundColor = UIColor.clearColor()
        btnSave.layer.cornerRadius = 25
        btnSave.layer.borderWidth = 3
        btnSave.layer.borderColor = UIColor().colorAccent().CGColor
        
        lblBarName.text = self.barName

        navigationItem.rightBarButtonItem = UIBarButtonItem(
            title: "Refresh",
            style: .Plain,
            target: self,
            action: #selector(refreshProgress)
        )
        
        savingLoadingAnimation.type = .BallGridPulse
        savingLoadingAnimation.hidesWhenStopped = true
        
        waitTimeLoadingAnimation.type = .BallGridPulse
        waitTimeLoadingAnimation.hidesWhenStopped = true
        
        crowdingLevelLoadingAnimation.type = .BallGridPulse
        crowdingLevelLoadingAnimation.hidesWhenStopped = true
        
        self.waitTimeProgressSlider.minimumValue = 0;
        self.waitTimeProgressSlider.maximumValue = 12;
        
        self.crowdingLevelProgressSlider.minimumValue = 0;
        self.crowdingLevelProgressSlider.maximumValue = 20;
    }
    
    // used this method so that the constraints would apply
    override func viewDidAppear(animated: Bool) {
        super.viewDidAppear(animated)
        
        waitTimeProgressBar.startAngle = -90
        waitTimeProgressBar.progressThickness = 0.2
        waitTimeProgressBar.trackThickness = 0.2
        waitTimeProgressBar.trackColor = UIColor().colorSecondaryText()
        waitTimeProgressBar.clockwise = true
        waitTimeProgressBar.center.y = progressBarsView.center.y
        waitTimeProgressBar.center.x = progressBarsView.center.x - 76.5
        waitTimeProgressBar.gradientRotateSpeed = 2
        waitTimeProgressBar.roundedCorners = true
        waitTimeProgressBar.glowMode = .Forward
        waitTimeProgressBar.glowAmount = 0
        waitTimeProgressBar.angle = 0
        waitTimeProgressBar.setColors(UIColor().progressGreen())
        scrollView.addSubview(waitTimeProgressBar)
        
        crowdingLevelProgressBar.startAngle = -90
        crowdingLevelProgressBar.progressThickness = 0.2
        crowdingLevelProgressBar.trackThickness = 0.2
        crowdingLevelProgressBar.trackColor = UIColor().colorSecondaryText()
        crowdingLevelProgressBar.clockwise = true
        crowdingLevelProgressBar.center.y = progressBarsView.center.y
        crowdingLevelProgressBar.center.x = progressBarsView.center.x + 76.5
        crowdingLevelProgressBar.gradientRotateSpeed = 2
        crowdingLevelProgressBar.roundedCorners = true
        crowdingLevelProgressBar.glowMode = .Forward
        crowdingLevelProgressBar.glowAmount = 0
        crowdingLevelProgressBar.angle = 0
        crowdingLevelProgressBar.setColors(UIColor().progressGreen())
        scrollView.addSubview(crowdingLevelProgressBar)
        
        refreshProgress()
    }

    override func didReceiveMemoryWarning() {
        super.didReceiveMemoryWarning()
        // Dispose of any resources that can be recreated.
    }
}
