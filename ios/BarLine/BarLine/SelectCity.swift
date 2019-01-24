import UIKit
import SwiftyJSON
import Alamofire
import Toast_Swift
import NVActivityIndicatorView
import Crashlytics

class SelectCity: UIViewController, UITableViewDataSource, UITableViewDelegate {

    @IBOutlet weak var navBar: UINavigationItem!
    @IBOutlet weak var loadingAnimation: NVActivityIndicatorView!
    @IBOutlet weak var lblProvince: UILabel!
    @IBOutlet weak var citiesTableView: UITableView!
    
    private var citiesArray: [String] = [String]()
    private var citiesLetterDictionaryArray: [String:[String]] = [String:[String]]()
    private var citiesLettersArray: [String] = [String]()
    
    var province = String()
    var country = String()
    var countryCode = String()
    
    func refresh() -> Void {
        self.citiesArray = [String]()
        self.citiesTableView.reloadData()
        
        self.loadingAnimation.startAnimation()
        Alamofire.request(.GET, ApiVariables.citiesUrl, parameters: ["country": country, "countryCode": countryCode, "province":province]).responseJSON{ (response)->Void in
                        
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
                        if let cityString: String = jsonObject["name"].stringValue {
                            self.citiesArray.append(cityString)
                        }
                    }
                    self.citiesLetterDictionaryArray = getLetterDictionaryArray(self.citiesArray)
                    self.citiesLettersArray = getLettersFromLettersDictionary(self.citiesLetterDictionaryArray)
                    
                    self.citiesTableView.reloadData()
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
    
    override func viewDidAppear(animated: Bool) {
        self.navigationController!.navigationBar.hidden = false
    }
    
    override func viewDidLoad() {
        super.viewDidLoad()
        
        navBar.rightBarButtonItem = UIBarButtonItem(title: "Refresh", style: .Plain, target: self, action: #selector(refresh))
        
        loadingAnimation.type = .BallGridPulse
        loadingAnimation.hidesWhenStopped = true;
        
        lblProvince.text = province
        
        citiesTableView.tableFooterView = UIView()
        
        refresh()
        
        self.citiesTableView.registerClass(UITableViewCell.self, forCellReuseIdentifier: "cell")
        self.citiesTableView.dataSource = self
        self.citiesTableView.delegate = self
    }
    
    // from UITableViewDataSource
    func tableView(tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        return citiesArray.count
    }
    func tableView(tableView: UITableView, cellForRowAtIndexPath indexPath: NSIndexPath) -> UITableViewCell {
        let cell = self.citiesTableView.dequeueReusableCellWithIdentifier("cell")! as UITableViewCell
        cell.textLabel!.text = self.citiesArray[indexPath.row]
        cell.backgroundColor = UIColor.clearColor()
        cell.textLabel?.textColor = UIColor().colorPrimaryText()
        let bgColorView = UIView()
        bgColorView.backgroundColor = UIColor().colorListSelect()
        cell.selectedBackgroundView = bgColorView
        
        cell.accessoryType = .DisclosureIndicator
        
        return cell
    }
    
    // from UITableViewDelegate
    func tableView(tableView: UITableView, didSelectRowAtIndexPath indexPath: NSIndexPath) {
        var city = self.citiesArray[indexPath.row]
        performSegueWithIdentifier("selectBarSegue", sender: city)
    }
    
    // pass data to bar
    override func prepareForSegue(segue: UIStoryboardSegue, sender: AnyObject?) {
        let selectBarVC : SelectBar = segue.destinationViewController as! SelectBar
        selectBarVC.city = sender as! String
        selectBarVC.province = self.province
        selectBarVC.country = self.country
        selectBarVC.countryCode = self.countryCode
    }
    
    override func didReceiveMemoryWarning() {
        super.didReceiveMemoryWarning()
        // Dispose of any resources that can be recreated.
    }

}
