import UIKit

extension UIColor {
    
    convenience init(red: Int, green: Int, blue: Int) {
        assert(red >= 0 && red <= 255, "Invalid red component")
        assert(green >= 0 && green <= 255, "Invalid green component")
        assert(blue >= 0 && blue <= 255, "Invalid blue component")
        
        self.init(red: CGFloat(red) / 255.0, green: CGFloat(green) / 255.0, blue: CGFloat(blue) / 255.0, alpha: 1.0)
    }
    
    convenience init(netHex:Int) {
        self.init(red:(netHex >> 16) & 0xff, green:(netHex >> 8) & 0xff, blue:netHex & 0xff)
    }
    
    func buttonTabUnselected() -> UIColor {
        return UIColor.clearColor()
    }
    
    func buttonTabSelected() -> UIColor {
        return UIColor.clearColor()
    }
    
    func buttonColorPressed() -> UIColor {
        return UIColor(netHex:0x34444B)
    }
    
    func colorAccent() -> UIColor {
        return UIColor(netHex:0x2196F3)
    }
    
    func colorAccentLight() -> UIColor {
        return UIColor(netHex:0x90CAF9)
    }
    
    func colorPrimary() -> UIColor {
        return UIColor(netHex:0x42526C)
    }
    
    func colorPrimaryDark() -> UIColor {
        return UIColor(netHex:0x344152)
    }
    
    func colorPrimaryLight() -> UIColor {
        return UIColor(netHex:0x777777)
    }
    
    func colorPrimaryText() -> UIColor {
        return UIColor(netHex:0xFFFFFF)
    }
    
    func colorSecondaryText() -> UIColor {
        return UIColor(netHex:0xB6B6B6)
    }
    
    func colorBackground() -> UIColor {
        return UIColor(netHex:0x303030)
    }
    
    func colorBackgroundDark() -> UIColor {
        return UIColor(netHex:0x151515)
    }
    
    func colorListSelect() -> UIColor {
        return UIColor(netHex:0x424242)
    }
    
    func progressRed() -> UIColor {
        return UIColor(netHex:0xF44336)
    }
    
    func progressYellow() -> UIColor {
        return UIColor(netHex:0xFFC107)
    }
    
    func progressGreen() -> UIColor {
        return UIColor(netHex:0x4CAF50)
    }

}
