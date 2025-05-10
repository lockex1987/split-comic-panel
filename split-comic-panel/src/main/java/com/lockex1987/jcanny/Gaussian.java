package com.lockex1987.jcanny;

/**
 * This class contains methods for masking image arrays with Gaussian masks.
 * Instead of convolving each pixel pixel with a 2D Gaussian kernel, it convolves
 * the image horizontally and vertically with a 1D Gaussian kernel.
 *
 * @author robert
 */

/*
TODO
-These can be combined into single method with more generic implementation
-Use height/width and radius for bounds checking
*/

public class Gaussian {

    // This seems like a very costly operation, only doing this once.
    private static final double SQRT2PI = Math.sqrt(2 * Math.PI);

    /**
     * Send this method an int[][] grayscale array, an int radius, and a double intensity to blur the
     * image with a Gaussian filter of that radius and intensity.
     *
     * @param raw    int[][], an array of grayscale values to be blurred
     * @param rad    int, the radius of the Gaussian filter (filter width = 2 * r + 1)
     * @param intens double, the intensity of the Gaussian blur
     * @return outRGB   int[][], an array of grayscale values from blurring input image with Gaussian filter
     */
    public static int[][] BlurGS(int[][] raw, int rad, double intens) {
        int height = raw.length;
        int width = raw[0].length;
        double norm = 0.;
        double intensSquared2 = 2 * intens * intens;
        //This also seems very costly, do it as little as possible
        double invIntensSqrPi = 1 / (SQRT2PI * intens);
        double[] mask = new double[2 * rad + 1];
        int[][] outGS = new int[height - 2 * rad][width - 2 * rad];

        // Create Gaussian kernel
        for (int x = -rad; x < rad + 1; x++) {
            double exp = Math.exp(-((x * x) / intensSquared2));
            mask[x + rad] = invIntensSqrPi * exp;
            norm += mask[x + rad];
        }

        // Convolve image with kernel horizontally
        for (int r = rad; r < height - rad; r++) {
            for (int c = rad; c < width - rad; c++) {
                double sum = 0.;
                for (int mr = -rad; mr < rad + 1; mr++) {
                    sum += (mask[mr + rad] * raw[r][c + mr]);
                }
                //Normalize channel after blur
                sum /= norm;
                outGS[r - rad][c - rad] = (int) Math.round(sum);
            }
        }

        // Convolve image with kernel vertically
        for (int r = rad; r < height - rad; r++) {
            for (int c = rad; c < width - rad; c++) {
                double sum = 0.;
                for (int mr = -rad; mr < rad + 1; mr++) {
                    sum += (mask[mr + rad] * raw[r + mr][c]);
                }
                //Normalize channel after blur
                sum /= norm;
                outGS[r - rad][c - rad] = (int) Math.round(sum);
            }
        }

        return outGS;
    }
}