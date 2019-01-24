import UIKit
import SwiftyJSON
import Alamofire
import Toast_Swift
import NVActivityIndicatorView
import Crashlytics

class SelectBar: UIViewController, UITableViewDataSource, UITableViewDelegate {

    @IBOutlet weak var loadingAnimation: NVActivityIndicatorView!
    @IBOutlet weak var navBar: UINavigationItem!
    @IBOutlet weak var barsTableView: UITableView!
    @IBOutlet weak var lblCity: UILabel!
    
    private var barsArray: [JSON] = [JSON]()
    private var barsLetterDictionaryArray: [String:[JSON]] = [String:[JSON]]()
    private var barsLettersArray: [String] = [String]()
    
    var city = String()
    var province = String()
    var country = String()
    var countryCode = String()
    
    func refresh() -> Void {
        self.barsArray = [JSON]()
        self.barsTableView.reloadData()
        
        self.loadingAnimation.startAnimation()
        Alamofire.request(.GET, ApiVariables.barsUrl, parameters: ["country": country, "countryCode": countryCode, "province":province, "city":city]).responseJSON{ (response)->Void in
            
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
                    
                    for jsonObject in json.arrayValue {
                        self.barsArray.append(jsonObject)
                    }
                    
                    self.barsLetterDictionaryArray = getLetterDictionaryJsonArray(self.barsArray)
                    self.barsLettersArray = getLettersFromLettersJsonDictionary(self.barsLetterDictionaryArray)
                    
                    self.barsTableView.reloadData()
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
    
    override func viewDidLoad() {
        super.viewDidLoad()
                
        navBar.rightBarButtonItem = UIBarButtonItem(title: "Refresh", style: .Plain, target: self, action: #selector(refresh))
        
        loadingAnimation.type = .BallGridPulse
        loadingAnimation.hidesWhenStopped = true;
        lblCity.text = self.city
        
        barsTableView.tableFooterView = UIView()
        
        refresh()
        
        self.barsTableView.registerClass(UITableViewCell.self, forCellReuseIdentifier: "cell")
        self.barsTableView.dataSource = self
        self.barsTableView.delegate = self
    }
    
    // from UITableViewDataSource
    func numberOfSectionsInTableView(tableView: UITableView) -> Int {
        return barsLetterDictionaryArray.count
    }
    func tableView(tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        return barsLetterDictionaryArray[barsLettersArray[section]]!.count;
    }
    func tableView(tableView: UITableView, cellForRowAtIndexPath indexPath: NSIndexPath) -> UITableViewCell {
        let cell = self.barsTableView.dequeueReusableCellWithIdentifier("cell")! as UITableViewCell
        
        cell.textLabel?.text = barsLetterDictionaryArray[barsLettersArray[indexPath.section]]![indexPath.row]["name"].stringValue;
        
        // styling the cell
        cell.backgroundColor = UIColor.clearColor()
        cell.textLabel?.textColor = UIColor().colorPrimaryText()
        let bgColorView = UIView()
        bgColorView.backgroundColor = UIColor().colorListSelect()
        cell.selectedBackgroundView = bgColorView
        cell.accessoryType = .DisclosureIndicator
        
        return cell
    }
    func tableView(tableView: UITableView, titleForHeaderInSection section: Int) -> String? {
        return barsLettersArray[section]
    }
    func tableView(tableView: UITableView, willDisplayHeaderView view: UIView, forSection section: Int) {
        let header: UITableViewHeaderFooterView = view as! UITableViewHeaderFooterView
        header.contentView.backgroundColor = UIColor().colorBackground()
        
        var bottomLine = CALayer()
        bottomLine.frame = CGRectMake(0.0, header.frame.height - 1, header.frame.width, 1.0)
        bottomLine.backgroundColor = UIColor().colorAccent().CGColor
        header.layer.addSublayer(bottomLine)
        
        header.textLabel!.textColor = UIColor().colorSecondaryText()
    }
    
    // from UITableViewDelegate
    func tableView(tableView: UITableView, didSelectRowAtIndexPath indexPath: NSIndexPath) {    
        let json = self.barsLetterDictionaryArray[barsLettersArray[indexPath.section]]![indexPath.row]
        var barDict = [String: String]()
        
        for (key, object) in json {
            barDict[key] = object.stringValue
        }
        
        performSegueWithIdentifier("waitTimeSegue", sender: barDict)
    }
    
    // pass data to wait time
    override func prepareForSegue(segue: UIStoryboardSegue, sender: AnyObject?) {
        let waitTimeVC : WaitTime = segue.destinationViewController as! WaitTime
        let barDict = sender as! [String: String]
        waitTimeVC.barName = barDict["name"]!
        waitTimeVC.barId = Int(barDict["id"]!)!
    }
    
    override func didReceiveMemoryWarning() {
        super.didReceiveMemoryWarning()
        // Dispose of any resources that can be recreated.
    }
}
