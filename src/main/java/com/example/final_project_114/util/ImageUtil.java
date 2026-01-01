package com.example.final_project_114.util;

import javafx.scene.image.Image;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;

public class ImageUtil {
    private static final Logger logger = LoggerFactory.getLogger(ImageUtil.class);
    private static final String DEFAULT_IMAGE_PATH = "/images/watches/default-watch.png";
    
    public static Image loadImage(String imageUrl, double width, double height, boolean preserveRatio) {
        if (imageUrl == null || imageUrl.trim().isEmpty()) {
            logger.debug("No image URL provided, returning null");
            return null;
        }
        
        try {
            if (imageUrl.startsWith("/")) {
                logger.info("Loading local resource image: {}", imageUrl);
                InputStream imageStream = ImageUtil.class.getResourceAsStream(imageUrl);
                
                if (imageStream != null) {
                    return new Image(imageStream, width, height, preserveRatio, true);
                } else {
                    logger.warn("Local resource not found: {}", imageUrl);
                    return null;
                }
            } else {
                logger.info("Loading external image: {}", imageUrl);
                return new Image(imageUrl, width, height, preserveRatio, true, true);
            }
        } catch (Exception e) {
            logger.error("Failed to load image: {}", imageUrl, e);
            return null;
        }
    }
    
    public static Image loadImage(String imageUrl) {
        return loadImage(imageUrl, 0, 0, true);
    }
    
    public static boolean isValidImageUrl(String imageUrl) {
        if (imageUrl == null || imageUrl.trim().isEmpty()) {
            return false;
        }
        
        Image testImage = loadImage(imageUrl, 1, 1, true);
        return testImage != null && !testImage.isError();
    }
    
    public static String getDefaultImagePath() {
        return DEFAULT_IMAGE_PATH;
    }
}
