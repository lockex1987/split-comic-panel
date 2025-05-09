package com.lockex1987;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;
import java.io.File;


public class InhanceImage {

    private BufferedImage image;

    public static void main(String[] args) throws Exception {
        new InhanceImage().run();
    }

    public void run() throws Exception {
        String folder = "/home/huyennv9/new/";
        String extension = ".jpg";
        String originalFileName = "06";

        image = ImageIO.read(new File(folder + originalFileName + extension));

        // image = upscale();
        // ImageIO.write(image, "jpg", new File(folder + originalFileName + " - 1 upscaled" + extension));

        // whiten();
        // increaseSharpness();
        // ImageIO.write(image, "jpg", new File(folder + originalFileName + " - 2 whitened" + extension));

        // makeSmoother();
        // blur();
        // antiAliasing();

        // image = thickenEdgeViaDilation();
        // invertColor();
    }

    private void whiten() {
        int threshold = 160;

        // (255 * 256 * 256) + (255 * 256) + 255 = 16711680 + 65280 + 255 = 16777215
        int whiteColor = 16777215;

        for (int x = 0; x < image.getWidth(); x++) {
            for (int y = 0; y < image.getHeight(); y++) {
                int argb = image.getRGB(x, y);
                // int alpha = (argb >> 24) & 0xFF;
                int red = (argb >> 16) & 0xFF;
                int green = (argb >> 8) & 0xFF;
                int blue = argb & 0xFF;
                if (red > threshold && green > threshold && blue > threshold) {
                    image.setRGB(x, y, whiteColor);
                }
            }
        }
    }

    private void increaseSharpness() {
        // Define the sharpening kernel
        float[] sharpenMatrix = {
            -1, -1, -1,
            -1, 9, -1,
            -1, -1, -1
        };
        Kernel kernel = new Kernel(3, 3, sharpenMatrix);
        ConvolveOp convolveOp = new ConvolveOp(kernel, ConvolveOp.EDGE_NO_OP, null);
        // Apply the convolution filter
        image = convolveOp.filter(image, null);
    }

    private void makeSmoother() {
        // Define the box blur kernel (3x3)
        float[] blurMatrix = {
            1 / 9f, 1 / 9f, 1 / 9f,
            1 / 9f, 1 / 9f, 1 / 9f,
            1 / 9f, 1 / 9f, 1 / 9f
        };
        Kernel kernel = new Kernel(3, 3, blurMatrix);
        ConvolveOp convolveOp = new ConvolveOp(kernel, ConvolveOp.EDGE_NO_OP, null);

        // Apply the convolution filter
        image = convolveOp.filter(image, null);
    }

    // Assume this function exists or you implement it
    private Kernel createGaussianKernel(int radius, float sigma) {
        int size = 2 * radius + 1;
        float[] matrix = new float[(int) Math.pow(size, 2)];
        float sigma22 = 2 * sigma * sigma;
        float sum = 0;
        int index = 0;

        for (int y = -radius; y <= radius; y++) {
            for (int x = -radius; x <= radius; x++) {
                float r2 = x * x + y * y;
                matrix[(index++)] = (float) (Math.exp(-r2 / sigma22) / (Math.PI * sigma22));
                sum += matrix[(index - 1)];
            }
        }

        // Normalize the kernel
        for (int i = 0; i < matrix.length; i++) {
            matrix[(i)] /= sum;
        }

        return new Kernel(size, size, matrix);
    }

    private void blur() {
        // Define Gaussian blur parameters
        int radius = 2; // Adjust for blur strength
        float sigma = 1.5f; // Adjust for blur smoothness

        // Create the Gaussian kernel
        Kernel gaussianKernel = createGaussianKernel(radius, sigma);
        ConvolveOp convolveOp = new ConvolveOp(gaussianKernel, ConvolveOp.EDGE_NO_OP, null);

        // Apply the convolution filter
        image = convolveOp.filter(image, null);
    }

    private void antiAliasing() {
        // Create a new BufferedImage with anti-aliasing enabled
        int width = image.getWidth();
        int height = image.getHeight();
        image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = image.createGraphics();

        // Enable anti-aliasing rendering hints
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // Draw the original image onto the new image with anti-aliasing
        g2d.drawImage(image, 0, 0, width, height, null);
        g2d.dispose();
    }

    private BufferedImage upscale() {
        int originalWidth = image.getWidth();
        int originalHeight = image.getHeight();

        // Define the target dimensions for upscaling (e.g., double the size)
        int ratio = 2;
        int targetWidth = originalWidth * ratio;
        int targetHeight = originalHeight * ratio;

        Object renderingHintValue;

        // Upscaling with Bilinear Interpolation
        // renderingHintValue = RenderingHints.VALUE_INTERPOLATION_BILINEAR;

        // Upscaling with Bicubic Interpolation
        renderingHintValue = RenderingHints.VALUE_INTERPOLATION_BICUBIC;

        // Upscaling with Nearest Neighbor Interpolation (for comparison - often pixelated)
        // renderingHintValue = RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR;

        // TYPE_INT_ARGB is png
        // If you use TYPE_INT_ARGB and ImageIO.write with jpg then no image file is created
        BufferedImage newImage = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = newImage.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, renderingHintValue);
        graphics.drawImage(image, 0, 0, targetWidth, targetHeight, null);
        graphics.dispose();
        return newImage;
    }


    private BufferedImage thickenEdgeViaDilation() {
        int width = image.getWidth();
        int height = image.getHeight();
        BufferedImage dilatedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        // Define the dilation kernel (structuring element)
        // A simple 3x3 cross or square kernel is common for dilation
        float[] dilationKernelData = {
            0, 1, 0,
            1, 1, 1,
            0, 1, 0
        };
        Kernel dilationKernel = new Kernel(3, 3, dilationKernelData);
        ConvolveOp dilationOp = new ConvolveOp(dilationKernel, ConvolveOp.EDGE_ZERO_FILL, null);

        // Apply the dilation
        BufferedImage tempImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        dilationOp.filter(image, tempImage);

        // The ConvolveOp doesn't directly perform a max operation like true dilation.
        // We need to iterate through the result and ensure the output pixel
        // is the maximum of the pixels under the kernel.

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int maxVal = 0;
                for (int ky = -1; ky <= 1; ky++) {
                    for (int kx = -1; kx <= 1; kx++) {
                        int nx = x + kx;
                        int ny = y + ky;
                        if (nx >= 0 && nx < width && ny >= 0 && ny < height && dilationKernelData[(ky + 1) * 3 + (kx + 1)] == 1) {
                            int pixel = image.getRGB(nx, ny);
                            int brightness = (new Color(pixel)).getRed(); // Assuming grayscale or focusing on brightness
                            maxVal = Math.max(maxVal, brightness);
                        }
                    }
                }
                dilatedImage.setRGB(x, y, new Color(maxVal, maxVal, maxVal).getRGB());
            }
        }

        return dilatedImage;
    }

    private void invertColor() {
        int width = image.getWidth();
        int height = image.getHeight();

        // Iterate through each pixel
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                // Get the RGB value of the current pixel
                int rgb = image.getRGB(x, y);
                Color color = new Color(rgb);

                // Invert each color component
                int red = 255 - color.getRed();
                int green = 255 - color.getGreen();
                int blue = 255 - color.getBlue();

                // Create a new Color with the inverted values
                Color invertedColor = new Color(red, green, blue);

                // Set the RGB value of the pixel in the inverted image
                image.setRGB(x, y, invertedColor.getRGB());
            }
        }
    }
}