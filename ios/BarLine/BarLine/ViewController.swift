import UIKit
import SwiftyJSON
import Alamofire
import Toast_Swift
import NVActivityIndicatorView
import Crashlytics

class ViewController: UIViewController {
    
    @IBOutlet weak var dialogBackgroundView: UIView!
    @IBOutlet weak var lblDialogTitle: UILabel!
    @IBOutlet weak var lblDialogMessage: UITextView!
    
    @IBOutlet var backgroundView: UIView!
    @IBOutlet weak var loadingAnimation: NVActivityIndicatorView!
    @IBOutlet weak var lblTitle: UILabel!
    
    @IBOutlet weak var btnCloseDialog: UIButton!
    
    @IBAction func btnCloseDialogPressed(sender: AnyObject) {
        btnCloseDialog.backgroundColor = UIColor().colorAccent()
    }
    @IBAction func btnCloseDialogReleased(sender: AnyObject) {
        btnCloseDialog.backgroundColor = UIColor.clearColor()
    }
    @IBAction func btnCloseDialog(sender: AnyObject) {
        btnCloseDialog.backgroundColor = UIColor.clearColor()
        closeDialog()
    }
    
    @IBOutlet weak var btnSearch: UIButton!
    
    @IBAction func btnSearchReleasedOutside(sender: AnyObject) {
        btnSearch.backgroundColor = UIColor.clearColor()
    }
    @IBAction func btnSearchPressed(sender: AnyObject) {
        btnSearch.backgroundColor = UIColor().colorAccent()
    }
    @IBAction func btnSearchReleased(sender: AnyObject) {
        btnSearch.backgroundColor = UIColor.clearColor()
        
        self.loadingAnimation.startAnimation()
        Alamofire.request(.GET, ApiVariables.statusUrl, parameters: [:]).responseJSON{ (response)->Void in
            switch response.result {
            case .Success:
                
                self.loadingAnimation.stopAnimation()
                
                if let value = response.result.value {
                    let json = JSON(value)
                    
                    if let statusCode = response.response?.statusCode {
                        if statusCode >= 400 {
                            self.view.makeToast(String(statusCode) + " - " + json["error"].stringValue)
                            self.loadingAnimation.stopAnimation()
                            return
                        }
                    }
                    
                    if let status: String = json["status"].stringValue {
                        
                        var message = ""
                        var title = ""
                        if json["message"].isExists() {
                            message = json["message"].stringValue
                        }
                        if json["title"].isExists() {
                            title = json["title"].stringValue
                        }
                        
                        if status == "ok" {
                            
                            if message != "" {
                                self.launchMessageDialog(false, message: message, title: title)
                            } else {
                                self.goToCitiesSelection()
                            }
                            
                        } else if status == "outdated" {
                            if message != "" {
                                self.launchMessageDialog(true, message: message, title: title)
                            } else {
                                self.view.makeToast("This app is out of date. Please update to to use it.")
                            }
                        } else {
                            self.goToCitiesSelection()
                        }
                    } else {
                        self.view.makeToast("500 - " + ErrorMessages.GENERIC_ERROR_MESSAGE)
                    }
                } else {
                    self.view.makeToast(ErrorMessages.CANT_GET_RESPONSE)
                }
            case .Failure(let error):
                self.view.makeToast("Error: " + error.localizedDescription)
                self.loadingAnimation.stopAnimation()
            }
        }

    }
    
    var apiOutdated = false
    
    func goToCitiesSelection() {
        self.performSegueWithIdentifier("citiesSegue", sender: nil)
    }
    
    func launchMessageDialog(apiOutdated: Bool, message: String, title: String) {        
        self.apiOutdated = apiOutdated
        
        dialogBackgroundView.alpha = 0.0
        
        dialogBackgroundView.hidden = false
        
        lblDialogTitle.text = title
        lblDialogMessage.text = message
        
        UIView.animateWithDuration(0.5, animations: {
            self.dialogBackgroundView.alpha = 1.0
            self.backgroundView.backgroundColor = UIColor().colorBackgroundDark()
        })
    }
    
    func closeDialog() {
        dialogBackgroundView.alpha = 0.0
        dialogBackgroundView.hidden = true
        backgroundView.backgroundColor = UIColor().colorBackground()
        
        //launch cities if allowed
        if (!self.apiOutdated) {
            goToCitiesSelection()
        }
    }
    
    override func viewDidAppear(animated: Bool) {
        self.navigationController!.navigationBar.hidden = true
    }
    
    override func viewDidLoad() {
        super.viewDidLoad()

        btnSearch.backgroundColor = UIColor.clearColor()
        btnSearch.layer.cornerRadius = 30
        btnSearch.layer.borderWidth = 3
        btnSearch.layer.borderColor = UIColor().colorAccent().CGColor
        
        loadingAnimation.type = .BallGridPulse
        loadingAnimation.hidesWhenStopped = true;
    
        btnCloseDialog.backgroundColor = UIColor.clearColor()
        btnCloseDialog.layer.cornerRadius = 10
        btnCloseDialog.layer.borderWidth = 3
        btnCloseDialog.layer.borderColor = UIColor().colorAccent().CGColor
    }

    override func didReceiveMemoryWarning() {
        super.didReceiveMemoryWarning()
        // Dispose of any resources that can be recreated.
    }
    
    override func prepareForSegue(segue: UIStoryboardSegue, sender: AnyObject?) {
        let selectCityVC : SelectCity = segue.destinationViewController as! SelectCity
        selectCityVC.province = "Ontario"
        selectCityVC.country = "Canada"
        selectCityVC.countryCode = "CA"
    }
}

