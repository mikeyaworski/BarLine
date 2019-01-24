//
//  BackgroundGradient.swift
//  BarLine
//
//  Created by Mike Yaworski on 2016-06-18.
//  Copyright © 2016 Mike Yaworski. All rights reserved.
//

import UIKit

extension CAGradientLayer {
    
    func backgroundGradientColor() -> CAGradientLayer {
        let topColor = UIColor(red: (0/255.0), green: (153/255.0), blue:(51/255.0), alpha: 1)
        let bottomColor = UIColor(red: (0/255.0), green: (153/255.0), blue:(255/255.0), alpha: 1)
        
        let gradientColors: [CGColor] = [topColor.CGColor, bottomColor.CGColor]
        let gradientLocations: [Float] = [0.0, 1.0]
        
        let gradientLayer: CAGradientLayer = CAGradientLayer()
        gradientLayer.colors = gradientColors
        gradientLayer.locations = gradientLocations
        
        return gradientLayer
        
    }
    
    func blueButtonGradient() -> CAGradientLayer {
        let topColor = UIColor(red: (51/255.0), green: (153/255.0), blue:(204/255.0), alpha: 1)
        let bottomColor = UIColor(red: (51/255.0), green: (153/255.0), blue:(170/255.0), alpha: 1)
        
        let gradientColors: [CGColor] = [topColor.CGColor, bottomColor.CGColor]
        let gradientLocations: [Float] = [0.0, 1.0]
        
        let gradientLayer: CAGradientLayer = CAGradientLayer()
        gradientLayer.colors = gradientColors
        gradientLayer.locations = gradientLocations
        
        return gradientLayer
        
    }
    
    func greenButtonGradient() -> CAGradientLayer {
        let topColor = UIColor(red: (0/255.0), green: (153/255.0), blue:(51/255.0), alpha: 1)
        let bottomColor = UIColor(red: (0/255.0), green: (204/255.0), blue:(68/255.0), alpha: 1)
        
        let gradientColors: [CGColor] = [topColor.CGColor, bottomColor.CGColor]
        let gradientLocations: [Float] = [0.0, 1.0]
        
        let gradientLayer: CAGradientLayer = CAGradientLayer()
        gradientLayer.colors = gradientColors
        gradientLayer.locations = gradientLocations
        
        return gradientLayer
        
    }

}
