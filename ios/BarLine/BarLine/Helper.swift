import UIKit
import Foundation
import SwiftyJSON
import Crashlytics

public struct ApiVariables {
    public static let baseUrl: String = "https://fluxuous.com/barline/api/v1"
    
    public static let citiesUrl: String = baseUrl + "/cities"
    public static let barsUrl: String = baseUrl + "/bars"
    public static let waitTimeUrl: String = baseUrl + "/wait_time"
    public static let authUrl: String = baseUrl + "/auth"
    public static let statusUrl: String = baseUrl + "/status"
}

public struct ErrorMessages {
    public static let GENERIC_ERROR_MESSAGE: String = "Something went wrong."
    public static let CANT_GET_RESPONSE: String = "Error: Can't get response value."
}

public func getAuthHeader(username: String, password: String) -> [String:String] {
    // set up the base64-encoded credentials
    let loginString = NSString(format: "%@:%@", username, password)
    let loginData: NSData = loginString.dataUsingEncoding(NSUTF8StringEncoding)!
    let base64LoginString = "Basic " + loginData.base64EncodedStringWithOptions([])
    
    let headers = [
        "Authorization": base64LoginString
    ]
    
    return headers
}

func getLastUpdatedStr(lastUpdatedDate: NSDate) -> String {
    let seconds = NSDate().timeIntervalSinceDate(lastUpdatedDate)
    var re = "over a day ago"
    
    if seconds < 60 {
        re = "just now";
    } else if (seconds < 3600) { // within hour
        let minutes = round(seconds / 60.0);
        var minutesString = " minutes ago";
        if (minutes < 2) {
            minutesString = " minute ago";
        }
        return String(Int(minutes)) + minutesString;
    } else if (seconds < 24 * 3600) { // within day
        let hours = round(seconds / 3600.0);
        var hoursString = " hours ago";
        if (hours < 2) {
            hoursString = " hour ago";
        }
        return String(Int(hours)) + hoursString;
    }
    return re
}

// e.g. returns ["a":["apple", "appeal"], "b":["banana"]] from ["apple", "appeal", "banana"]
func getLetterDictionaryArray(array: [String]) -> [String:[String]] {
    var dict: [String: [String]] = [String:[String]]()
    for str in array {
        var firstChar = String(str.characters.first!)
        if str.uppercaseString.hasPrefix(firstChar.uppercaseString) {
            if (dict[firstChar.uppercaseString] == nil) {
               dict[firstChar.uppercaseString] = [str]
            } else {
                dict[firstChar.uppercaseString]!.append(str)
            }
        }
    }
    return dict
}

// e.g. returns ["a", "b"] from ["a":["apple", "appeal"], "b":["banana"]]
func getLettersFromLettersDictionary(dictionary: [String:[String]]) -> [String] {
    var lettersArr: [String] = [String]()
    for (letter, citiesArray) in dictionary {
        lettersArr.append(letter)
    }
    return lettersArr.sort { $0 < $1 }
}

// same as getLetterDictionaryArray except parameter is [["name":"apple"...], ["name":"banana"...]]
func getLetterDictionaryJsonArray(array: [JSON]) -> [String:[JSON]] {
    var dict: [String: [JSON]] = [String:[JSON]]()
    
    for jObj in array {
        var firstChar = String(jObj["name"].stringValue.characters.first!)
        if jObj["name"].stringValue.uppercaseString.hasPrefix(firstChar.uppercaseString) {
            if (dict[firstChar.uppercaseString] == nil) {
                dict[firstChar.uppercaseString] = [jObj]
            } else {
                dict[firstChar.uppercaseString]!.append(jObj)
            }
        }
    }
    return dict
}

// same as getLettersFromLettersDictionary except parameter is
// ["a":[["name":"apple"...], ["name":"banana"...]], "b":[["name":"carrot"...]]]
func getLettersFromLettersJsonDictionary(dictionary: [String:[JSON]]) -> [String] {
    var lettersArr: [String] = [String]()
    
    for (letter, jArray) in dictionary {
        lettersArr.append(letter)
    }
    
    return lettersArr.sort { $0 < $1 }
}
