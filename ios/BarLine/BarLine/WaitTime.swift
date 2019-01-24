//
//  WaitTime.swift
//  BarLine
//
//  Created by Mike Yaworski on 2016-07-09.
//  Copyright © 2016 Mike Yaworski. All rights reserved.
//

import UIKit
import SwiftyJSON
import Alamofire
import Toast_Swift
import NVActivityIndicatorView
//import QuartzCore
import KDCircularProgress
import Foundation
import Crashlytics

class WaitTime: UIViewController {
    
    @IBOutlet weak var scrollView: UIScrollView!
    @IBOutlet weak var btnCrowdingLevel: UIButton!
    @IBOutlet weak var btnWaitTime: UIButton!
    
    @IBOutlet weak var btnEventDetails: UIButton!
    @IBAction func btnEventDetailsReleased(sender: AnyObject) {
        showAd()
    }
    
    @IBOutlet weak var adOpaqueBackground: UIView!
    @IBOutlet weak var adBackground: UIView!
    @IBOutlet weak var lblAdText: UITextView!
    @IBOutlet weak var adLoadingAnimation: NVActivityIndicatorView!
    @IBOutlet weak var adImgView: UIImageView!
    
    @IBOutlet weak var btnCloseAd: UIButton!
    @IBAction func btnCloseAdPressed(sender: AnyObject) {
        btnCloseAd.backgroundColor = UIColor().colorAccent()
    }
    @IBAction func btnCloseAdReleasedOutside(sender: AnyObject) {
        btnCloseAd.backgroundColor = UIColor.clearColor()
    }
    @IBAction func closeAd(sender: AnyObject) {
        btnCloseAd.backgroundColor = UIColor.clearColor()
        closeAd()
    }
    
    @IBOutlet weak var btnFindOutMore: UIButton!
    @IBAction func findOutMore(sender: AnyObject) {
        btnFindOutMore.backgroundColor = UIColor.clearColor()
        if adLink != "" {
            UIApplication.sharedApplication().openURL(NSURL(string: adLink)!)
        }
        
        var tracker = GAI.sharedInstance().defaultTracker
        tracker.send(GAIDictionaryBuilder.createEventWithCategory("ad", action: "Find Out More: " + String(barId), label: "Text: " + adText + "\nImage: " + adImg + "\nLink: " + adLink, value: nil).build() as [NSObject : AnyObject])
    }
    @IBAction func btnFindOutMorePressed(sender: AnyObject) {
        btnFindOutMore.backgroundColor = UIColor().colorAccent()
    }
    @IBAction func btnFindOutMoreReleasedOutside(sender: AnyObject) {
        btnFindOutMore.backgroundColor = UIColor.clearColor()
    }
    
    @IBOutlet weak var lblProgress: UILabel!
    @IBOutlet weak var progressView: UIView!
    @IBOutlet weak var navBar: UINavigationItem!
    @IBOutlet weak var loadingAnimation: NVActivityIndicatorView!
    @IBOutlet weak var editLoadingAnimation: NVActivityIndicatorView!
    @IBOutlet weak var lblLastUpdated: UILabel!
    @IBOutlet weak var lblCurrentTime: UILabel!
    @IBOutlet weak var lblBarName: UILabel!
    
    var barName = String()
    var barId = Int()
    
    var waitTime = Int()
    var crowdingLevel = Int()
    
    var adText = String()
    var adImg = String()
    var adLink = String()
    
    var progressBar = KDCircularProgress(frame: CGRect(x: 0, y: 0, width: 200, height: 200))
    
    func refreshProgress(isWaitTime: Bool, isFirstRefresh: Bool)->Void {
        self.loadingAnimation.startAnimation()
        progressBar.angle = 0
        lblProgress.text = ""
        
        // commented out because I think it was causing the API calls to sometimes never load
        // attempt to delete API call cache. iOS sucks.
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
                            self.loadingAnimation.stopAnimation()
                            return
                        }
                    }
                    
                    let jsonObject = json;
                    self.waitTime = jsonObject["wait_time"].intValue
                    self.crowdingLevel = jsonObject["crowding_level"].intValue
                    self.populateProgressBar(isWaitTime)
                    
                    self.adText = jsonObject["ad_text"].stringValue
                    self.lblAdText.text = self.adText
                    if self.adText != "" {
                        self.btnEventDetails.hidden = false
                    }
                    
                    self.adLink = jsonObject["ad_link"].stringValue
                    
                    self.adImg = jsonObject["ad_img"].stringValue
                    if self.adImg != "" {
                        self.btnEventDetails.hidden = false
                        self.loadAd(self.adImg, adLink: self.adLink)
                    }
    
                    if (isFirstRefresh && (self.adText != "" || self.adImg != "")) {
                        self.showAdIfNoAuth()
                    }
                    
                    
                    // -0700 since it is stored as PST in server
                    let lastUpdatedFromAPI: String = jsonObject["last_updated"].stringValue + " -0700"
                    let dateFormatter = NSDateFormatter()
                    dateFormatter.dateFormat = "yyyy-MM-dd HH:mm:ss Z"
                    let date = dateFormatter.dateFromString(lastUpdatedFromAPI)
                    self.lblLastUpdated.text = "Last updated: " + getLastUpdatedStr(date!)
                    self.updateCurrentTime()
                    
                    self.loadingAnimation.stopAnimation()
                } else {
                    self.view.makeToast(ErrorMessages.CANT_GET_RESPONSE)
                }
            case .Failure(let error):
                self.view.makeToast("Error: " + error.localizedDescription)
                self.loadingAnimation.stopAnimation()
            }
        }

    }
    
    func refreshProgressFromSelector() {
        refreshProgress(self.btnWaitTime.layer.borderWidth > 0, isFirstRefresh: false)
    }
    
    func updateCurrentTime() {
        let currentDate = NSDate()
        lblCurrentTime.text = currentDate.toShortTimeString()
    }
    
    func populateProgressBar(isWaitTime: Bool) {
        
        btnWaitTime.backgroundColor = UIColor.clearColor()
        btnWaitTime.layer.cornerRadius = 5
        btnWaitTime.layer.borderWidth = 2
        btnWaitTime.layer.borderColor = UIColor().colorAccent().CGColor
        
        btnCrowdingLevel.backgroundColor = UIColor.clearColor()
        btnCrowdingLevel.layer.cornerRadius = 5
        btnCrowdingLevel.layer.borderWidth = 2
        btnCrowdingLevel.layer.borderColor = UIColor().colorAccent().CGColor
        
        if isWaitTime {
            
            self.btnWaitTime.backgroundColor = UIColor().buttonTabSelected()
            self.btnCrowdingLevel.backgroundColor = UIColor().buttonTabUnselected()
            
            btnCrowdingLevel.layer.borderWidth = 0
            
            var waitTimeStr = "\(self.waitTime) mins"
            if self.waitTime == 60 {
                waitTimeStr = "60+ mins"
            } else if (self.waitTime == 0) {
                waitTimeStr = "No wait!"
            }
            self.lblProgress.text = waitTimeStr
            
            let percentage = Double(self.waitTime) / 60.0
            let angle = percentage * 360
            
            // timer to change colour of the progress bar (only while animating)
            /*var animatingTimer = NSTimer.scheduledTimerWithTimeInterval(
                0.1,
                target: self,
                selector: #selector(updateProgressColour),
                userInfo: nil,
                repeats: true
            )*/
            
            self.progressBar.animateToAngle(angle, duration: 1, completion: { (Bool) in
                /*self.updateProgressColours()
                 animatingTimer.invalidate()*/
            })
            updateProgressColour()
        
        }
        if !isWaitTime {
            
            self.btnCrowdingLevel.backgroundColor = UIColor().buttonTabSelected()
            self.btnWaitTime.backgroundColor = UIColor().buttonTabUnselected()
            
            btnWaitTime.layer.borderWidth = 0
            
            var crowdingLevelStr = "\(self.crowdingLevel)%"
            if self.crowdingLevel >= 100 {
                crowdingLevelStr = "Full"
            } else if (self.crowdingLevel <= 0) {
                crowdingLevelStr = "Empty"
            }
            self.lblProgress.text = crowdingLevelStr
            
            let percentage = Double(self.crowdingLevel) / 100.0
            let angle = percentage * 360
            
            // timer to change colour of the progress bar (only while animating)
            /*var animatingTimer = NSTimer.scheduledTimerWithTimeInterval(
                0.1,
                target: self,
                selector: #selector(updateProgressColour),
                userInfo: nil,
                repeats: true
            )*/
            
            self.progressBar.animateToAngle(angle, duration: 1, completion: { (Bool) in
                /*self.updateProgressColours()
                animatingTimer.invalidate()*/
            })
            updateProgressColour()
        
        }
    }
    
    // currently, this function is called at the appropriate times
    // but I have no way of getting the value of the progressBar while it's animating
    // (.angle returns the value that it is animating to, no matter what point it's at in the animation)
    // so I've asked the creator to implement a feature where I can get the value of the angle while it's animating
    func updateProgressColour() {
        let percentage = progressBar.angle / 360.0;
        
        if (percentage <= Double(1) / Double(3)) {
            self.progressBar.setColors(UIColor().progressGreen())
        } else if (percentage <= Double(2) / Double(3)) {
            self.progressBar.setColors(UIColor().progressYellow())
        } else if (percentage <= Double(3) / Double(3)) {
            self.progressBar.setColors(UIColor().progressRed())
        }
    }
    
    @IBAction func btnWaitTime(sender: AnyObject) {
        populateProgressBar(true)
    }    
    @IBAction func btnCrowdingLevel(sender: AnyObject) {
        populateProgressBar(false)
    }
    
    func loadAd(url: String, adLink: String) {
        if url != "" {
            adImgView.downloadedFrom(link: url, loadingAnimation: self.adLoadingAnimation)
        }
    }
    
    func showAd() {
        
        // log that the ad is being shown (to Google Analytics)
        let tracker = GAI.sharedInstance().defaultTracker
        tracker.set(kGAIScreenName, value: "Ad-" + String(self.barId))
        let builder = GAIDictionaryBuilder.createScreenView()
        tracker.send(builder.build() as [NSObject : AnyObject])
        
        adOpaqueBackground.alpha = 0.0
        adOpaqueBackground.hidden = false
        
        if adLink != ""{
            btnFindOutMore.hidden = false
        } else {
            btnFindOutMore.hidden = true
        }
        
        UIView.animateWithDuration(0.5, animations: {
            self.adOpaqueBackground.alpha = 1.0
        })
    }
    
    func showAdIfNoAuth() {
        
        let preferences = NSUserDefaults.standardUserDefaults()
        let passwordKey = "password-" + String(self.barId)
        if preferences.objectForKey(passwordKey) != nil {
            let password = preferences.stringForKey(passwordKey)
            
            let headers = getAuthHeader(String(barId), password: password!)
            
            Alamofire.request(.GET, ApiVariables.authUrl + "/" + String(self.barId), headers: headers, parameters: [:]).responseJSON{ (response)->Void in
                                
                switch response.result {
                case .Success:
                    if let value = response.result.value {
                        let json = JSON(value)
                        
                        if let statusCode = response.response?.statusCode {
                            if statusCode >= 400 {
                                self.showAd()
                                return
                            }
                        }
                        
                        let authStatus = json["auth_status"].intValue;
                        
                        if (authStatus != 1) {
                            self.showAd()
                        }
                        
                    }
                case .Failure(let error):
                    self.showAd()
                }
            }
            
        } else {
            showAd()
        }
        
    }
    
    func closeAd() {
        adOpaqueBackground.alpha = 0.0
        adOpaqueBackground.hidden = true
    }
    
    var isFirstLoad = false;
    
    override func viewWillAppear(animated: Bool) {
        super.viewWillAppear(animated)
        
        refreshProgress(true, isFirstRefresh: isFirstLoad)
        isFirstLoad = false
    }
        
    override func viewDidLoad() {
        super.viewDidLoad()
        
        isFirstLoad = true
        
        btnCloseAd.backgroundColor = UIColor.clearColor()
        btnCloseAd.layer.cornerRadius = 15
        btnCloseAd.layer.borderWidth = 3
        btnCloseAd.layer.borderColor = UIColor().colorAccent().CGColor
        
        btnFindOutMore.backgroundColor = UIColor.clearColor()
        btnFindOutMore.layer.cornerRadius = 15
        btnFindOutMore.layer.borderWidth = 3
        btnFindOutMore.layer.borderColor = UIColor().colorAccent().CGColor
        
        let isWaitTime = true
        let refreshBtn = UIBarButtonItem(
            title: "Refresh",
            style: .Plain,
            target: self,
            action: #selector(refreshProgressFromSelector)
        )
        
        let editBtn = UIBarButtonItem(title: "Edit", style: .Plain, target: self, action: #selector(gotoEdit))
        navigationItem.rightBarButtonItems = [editBtn, refreshBtn]
        
        loadingAnimation.type = .BallGridPulse
        loadingAnimation.hidesWhenStopped = true
        
        editLoadingAnimation.type = .BallGridPulse
        editLoadingAnimation.hidesWhenStopped = true
        
        adLoadingAnimation.type = .BallGridPulse
        adLoadingAnimation.hidesWhenStopped = true

        lblBarName.text = self.barName
        
        progressBar.startAngle = -90
        progressBar.progressThickness = 0.2
        progressBar.trackThickness = 0.2
        progressBar.trackColor = UIColor().colorSecondaryText()
        progressBar.clockwise = true
        progressBar.center.y = progressView.center.y
        progressBar.center.x = view.center.x
        progressBar.gradientRotateSpeed = 2
        progressBar.roundedCorners = true
        progressBar.glowMode = .Forward
        progressBar.glowAmount = 0
        progressBar.angle = 0
        progressBar.setColors(UIColor().progressGreen())
        scrollView.addSubview(progressBar)
        
        adOpaqueBackground.layer.zPosition = CGFloat.max // bring to front (shown over all other views)
    }
    
    func gotoEdit() {
        gotoEditWithParam(false);
    }
    
    // can't overload or have optional parameter in this function because selector calls it and doesn't
    // convienently support argument variations
    func gotoEditWithParam(hasFailed: Bool)->Void {
        
        closeAd() // so that the toasts will appear if passwor is incorrect
        
        var alertController:UIAlertController?
        alertController = UIAlertController(title: "",
                                            message: "",
                                            preferredStyle: .Alert)
        
        // changing the title color
        let attributedString = NSAttributedString(string: "Password", attributes: [
            NSForegroundColorAttributeName : UIColor().colorPrimaryText()
            ])
        alertController!.setValue(attributedString, forKey: "attributedTitle")
        
        let subview = alertController!.view.subviews.first! as UIView
        let alertContentView = subview.subviews.first! as UIView
        alertContentView.backgroundColor = UIColor().colorBackground()
        alertController!.view.tintColor = UIColor().colorPrimaryText();
        
        alertController!.addTextFieldWithConfigurationHandler({
            (textField: UITextField!) in
            textField.attributedPlaceholder = NSAttributedString(string:"Password", attributes:[NSForegroundColorAttributeName: UIColor.blackColor()])
            textField.secureTextEntry = true
            textField.textColor = UIColor.blackColor()
            
            if !hasFailed {
                let preferences = NSUserDefaults.standardUserDefaults()
                let passwordKey = "password-" + String(self.barId)
                if preferences.objectForKey(passwordKey) != nil {
                    let password = preferences.stringForKey(passwordKey)
                    textField.text = password
                }
            }
            
            // superview doesn't work for some reason (breaks)
            //textField.backgroundColor = UIColor.clearColor();
            //textField.superview!.backgroundColor = UIColor.clearColor();
        })
        
        /*for textfield: UIView in alertController.textfields {
         var container: UIView = textField.superview
         var effectView: UIView = container.superview.subviews[0]
         container.backgroundColor = UIColor.clearColor()
         effectView.removeFromSuperview()
         }*/
        
        let action = UIAlertAction(title: "Submit",
                                   style: UIAlertActionStyle.Default,
                                   handler: {
                                    (paramAction:UIAlertAction!) in
                                    if let textFields = alertController?.textFields{
                                        let theTextFields = textFields as [UITextField]
                                        let enteredText = theTextFields[0].text
                                        self.authForEdit(enteredText!)
                                    }
        })
        
        let cancelAction = UIAlertAction(title: "Cancel",
                                         style: UIAlertActionStyle.Cancel,
                                         handler: nil);
        
        alertController?.addAction(action)
        alertController?.addAction(cancelAction)
        
        if (!hasFailed && alertController?.textFields![0].text != "") {
            self.authForEdit((alertController?.textFields![0].text)!)
        } else {
            self.presentViewController(alertController!, animated: true, completion: nil)
        }
    }
    
    func authForEdit(password: String) {
        self.editLoadingAnimation.startAnimation()
        Alamofire.request(.GET, ApiVariables.authUrl + "/" + String(self.barId), headers: getAuthHeader(String(barId), password: password), parameters: [:]).responseJSON{ (response)->Void in
            
            switch response.result {
            case .Success:
                if let value = response.result.value {
                    let json = JSON(value)
                    
                    if let statusCode = response.response?.statusCode {
                        if statusCode >= 400 {
                            
                            if json["error"].isExists() {
                                self.view.makeToast(String(statusCode) + " - " + json["error"].stringValue)
                            } else if json["auth_status"].isExists() { // no error value, so probably incorrect password
                                self.view.makeToast(json["message"].stringValue)
                            } else {
                                self.view.makeToast(String(statusCode) + " - " + ErrorMessages.GENERIC_ERROR_MESSAGE)
                            }
                            self.editLoadingAnimation.stopAnimation()
                            return
                        }
                    }
                    
                    let authStatus = json["auth_status"].intValue;
                    
                    if (authStatus == 1) {
                        let preferences = NSUserDefaults.standardUserDefaults()
                        let passwordKey = "password-" + String(self.barId)
                        preferences.setObject(password, forKey: passwordKey)
                        preferences.synchronize() // save to disk
                        
                        self.performSegueWithIdentifier("editSegue", sender: password)
                    } else {
                        self.view.makeToast(json["message"].stringValue);
                        self.gotoEditWithParam(true)
                    }
                    
                    self.editLoadingAnimation.stopAnimation()
                }
            case .Failure(let error):
                self.view.makeToast("Error: " + error.localizedDescription)
                self.editLoadingAnimation.stopAnimation()
            }
        }
    }
    
    // pass data to edit
    override func prepareForSegue(segue: UIStoryboardSegue, sender: AnyObject?) {
        let editVC : Edit = segue.destinationViewController as! Edit
        editVC.barName = self.barName
        editVC.barId = self.barId
        editVC.password = sender as! String
        editVC.waitTimeView = self.view
    }

    override func didReceiveMemoryWarning() {
        super.didReceiveMemoryWarning()
        // Dispose of any resources that can be recreated.
    }
}
