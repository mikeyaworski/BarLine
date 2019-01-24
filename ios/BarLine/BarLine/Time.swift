import UIKit

extension NSDate {
    
    func hour() -> Int {
        let calendar = NSCalendar.currentCalendar()
        let components = calendar.components(.Hour, fromDate: self)
        let hour = components.hour
        return hour
    }
    
    func minute() -> Int {
        let calendar = NSCalendar.currentCalendar()
        let components = calendar.components(.Minute, fromDate: self)
        let minute = components.minute
        return minute
    }
    
    func toShortTimeString() -> String {
        let formatter = NSDateFormatter()
        formatter.timeStyle = .ShortStyle
        let timeString = formatter.stringFromDate(self)
        return timeString
    }
}