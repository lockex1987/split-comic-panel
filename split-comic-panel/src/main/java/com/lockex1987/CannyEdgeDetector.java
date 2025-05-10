package com.lockex1987;

import java.awt.*;
import java.awt.image.BufferedImage;


public class CannyEdgeDetector {

    private int width;
    private int height;
    private float[][] gaussianKernel;
    private float[][] gradientMagnitude;
    private float[][] gradientAngle;
    private boolean[][] nonMaxSuppressed;
    private boolean[][] thresholdedEdges;
    private boolean[][] finalEdges;

    public void detectEdges(BufferedImage image, double sigma, double lowThreshold, double highThreshold) {
        width = image.getWidth();
        height = image.getHeight();
        gaussianKernel = createGaussianKernel(sigma);
        gradientMagnitude = new float[height][width];
        gradientAngle = new float[height][width];
        nonMaxSuppressed = new boolean[height][width];
        thresholdedEdges = new boolean[height][width];
        finalEdges = new boolean[height][width];

        // 1. Gaussian Blur
        BufferedImage blurredImage = applyGaussianBlur(image);

        // 2. Gradient Calculation (Sobel Operator)
        calculateGradient(blurredImage);

        // 3. Non-Maximum Suppression
        applyNonMaximumSuppression();

        // 4. Double Thresholding
        applyDoubleThreshold(lowThreshold, highThreshold);

        // 5. Edge Tracking by Hysteresis
        trackEdges(highThreshold);
    }

    private BufferedImage applyGaussianBlur(BufferedImage image) {
        int kernelSize = gaussianKernel.length;
        int kernelRadius = kernelSize / 2;
        BufferedImage blurredImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                float redSum = 0;
                float greenSum = 0;
                float blueSum = 0;
                float weightSum = 0;
                for (int ky = -kernelRadius; ky <= kernelRadius; ky++) {
                    for (int kx = -kernelRadius; kx <= kernelRadius; kx++) {
                        int sampleX = Math.min(width - 1, Math.max(0, x + kx));
                        int sampleY = Math.min(height - 1, Math.max(0, y + ky));
                        Color pixel = new Color(image.getRGB(sampleX, sampleY));
                        float weight = gaussianKernel[ky + kernelRadius][kx + kernelRadius];
                        redSum += pixel.getRed() * weight;
                        greenSum += pixel.getGreen() * weight;
                        blueSum += pixel.getBlue() * weight;
                        weightSum += weight;
                    }
                }
                int red = Math.round(redSum / weightSum);
                int green = Math.round(greenSum / weightSum);
                int blue = Math.round(blueSum / weightSum);
                blurredImage.setRGB(x, y, new Color(red, green, blue).getRGB());
            }
        }
        return blurredImage;
    }

    private float[][] createGaussianKernel(double sigma) {
        int size = (int) (6 * sigma + 1);
        if (size % 2 == 0) {
            size++;
        }
        int radius = size / 2;
        float[][] kernel = new float[size][size];
        double sum = 0.0;

        for (int y = -radius; y <= radius; y++) {
            for (int x = -radius; x <= radius; x++) {
                double exponent = -(x * x + y * y) / (2 * sigma * sigma);
                kernel[y + radius][x + radius] = (float) (Math.exp(exponent) / (2 * Math.PI * sigma * sigma));
                sum += kernel[y + radius][x + radius];
            }
        }

        // Normalize the kernel
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                kernel[y][x] /= sum;
            }
        }
        return kernel;
    }

    private void calculateGradient(BufferedImage image) {
        int[][] sobelX = {{-1, 0, 1}, {-2, 0, 2}, {-1, 0, 1}};
        int[][] sobelY = {{-1, -2, -1}, {0, 0, 0}, {1, 2, 1}};

        for (int y = 1; y < height - 1; y++) {
            for (int x = 1; x < width - 1; x++) {
                float gradientXRed = 0, gradientYRed = 0;
                float gradientXGreen = 0, gradientYGreen = 0;
                float gradientXBlue = 0, gradientYBlue = 0;

                for (int ky = -1; ky <= 1; ky++) {
                    for (int kx = -1; kx <= 1; kx++) {
                        Color pixel = new Color(image.getRGB(x + kx, y + ky));
                        gradientXRed += pixel.getRed() * sobelX[ky + 1][kx + 1];
                        gradientYRed += pixel.getRed() * sobelY[ky + 1][kx + 1];
                        gradientXGreen += pixel.getGreen() * sobelX[ky + 1][kx + 1];
                        gradientYGreen += pixel.getGreen() * sobelY[ky + 1][kx + 1];
                        gradientXBlue += pixel.getBlue() * sobelX[ky + 1][kx + 1];
                        gradientYBlue += pixel.getBlue() * sobelY[ky + 1][kx + 1];
                    }
                }

                float magnitude = (float) Math.sqrt(gradientXRed * gradientXRed + gradientYRed * gradientYRed +
                    gradientXGreen * gradientXGreen + gradientYGreen * gradientYGreen +
                    gradientXBlue * gradientXBlue + gradientYBlue * gradientYBlue) / 3f;
                gradientMagnitude[y][x] = magnitude;
                gradientAngle[y][x] = (float) Math.toDegrees(Math.atan2(gradientYRed + gradientYGreen + gradientYBlue,
                    gradientXRed + gradientXGreen + gradientXBlue));
                if (gradientAngle[y][x] < 0) {
                    gradientAngle[y][x] += 360;
                }
            }
        }
    }

    private void applyNonMaximumSuppression() {
        for (int y = 1; y < height - 1; y++) {
            for (int x = 1; x < width - 1; x++) {
                float currentMagnitude = gradientMagnitude[y][x];
                float angle = gradientAngle[y][x];
                float neighbor1 = 0, neighbor2 = 0;

                if ((angle >= 0 && angle < 22.5) || (angle >= 157.5 && angle < 202.5) || (angle >= 337.5 && angle <= 360)) {
                    neighbor1 = gradientMagnitude[y][x + 1];
                    neighbor2 = gradientMagnitude[y][x - 1];
                } else if ((angle >= 22.5 && angle < 67.5) || (angle >= 202.5 && angle < 247.5)) {
                    neighbor1 = gradientMagnitude[y + 1][x - 1];
                    neighbor2 = gradientMagnitude[y - 1][x + 1];
                } else if ((angle >= 67.5 && angle < 112.5) || (angle >= 247.5 && angle < 292.5)) {
                    neighbor1 = gradientMagnitude[y + 1][x];
                    neighbor2 = gradientMagnitude[y - 1][x];
                } else if ((angle >= 112.5 && angle < 157.5) || (angle >= 292.5 && angle < 337.5)) {
                    neighbor1 = gradientMagnitude[y + 1][x + 1];
                    neighbor2 = gradientMagnitude[y - 1][x - 1];
                }

                if (currentMagnitude >= neighbor1 && currentMagnitude >= neighbor2) {
                    nonMaxSuppressed[y][x] = true;
                }
            }
        }
    }

    private void applyDoubleThreshold(double lowThreshold, double highThreshold) {
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (nonMaxSuppressed[y][x]) {
                    if (gradientMagnitude[y][x] >= highThreshold) {
                        thresholdedEdges[y][x] = true; // Strong edge
                    } else if (gradientMagnitude[y][x] >= lowThreshold) {
                        thresholdedEdges[y][x] = false; // Weak edge (initially false, might be turned true by hysteresis)
                    }
                }
            }
        }
    }

    private void trackEdges(double highThreshold) {
        for (int y = 1; y < height - 1; y++) {
            for (int x = 1; x < width - 1; x++) {
                if (thresholdedEdges[y][x]) {
                    // Found a strong edge, start tracking
                    finalEdges[y][x] = true;
                    recursivelyTrack(x, y);
                }
            }
        }
    }

    private void recursivelyTrack(int x, int y) {
        for (int ky = -1; ky <= 1; ky++) {
            for (int kx = -1; kx <= 1; kx++) {
                if (kx == 0 && ky == 0) continue;
                int nx = x + kx;
                int ny = y + ky;
                if (nx >= 0 && nx < width && ny >= 0 && ny < height && thresholdedEdges[ny][nx] && !finalEdges[ny][nx]) {
                    finalEdges[ny][nx] = true;
                    recursivelyTrack(nx, ny);
                }
            }
        }
    }

    public BufferedImage getEdgesImage() {
        BufferedImage edgesImage = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_BINARY);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (finalEdges[y][x]) {
                    edgesImage.setRGB(x, y, Color.WHITE.getRGB());
                } else {
                    edgesImage.setRGB(x, y, Color.BLACK.getRGB());
                }
            }
        }
        return edgesImage;
    }
}