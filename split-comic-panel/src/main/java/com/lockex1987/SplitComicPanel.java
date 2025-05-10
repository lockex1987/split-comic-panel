package com.lockex1987;

import com.lockex1987.bean.Cell;
import com.lockex1987.bean.Row;
import com.lockex1987.jcanny.JCanny;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


public class SplitComicPanel {

    private static final int BACKGROUND_COLOR = -16777216;

    private final String folder = "/home/lockex1987/new/01/";

    // Adjust to get optimized values
    private final int minimumWidth = 300;
    private final int minimumHeight = 100;
    private final int continuousRowsGap = 15;

    // Parameters for Canny edge detection
    private final double sigma = 1.4;
    private final double lowThreshold = 10;
    private final double highThreshold = 30;

    // With the old method for Canny edge detection, grascale image is smaller than original image (offset = 9),
    // but with the new method, two images are equal
    int offset = 0;

    public static void main(String[] args) throws Exception {
        new SplitComicPanel().run();
    }

    public void run() throws Exception {
        for (int page = 50; page <= 50; page++) {
            String originalFileName = String.format("%03d", page);
            // webp format is not supported
            String extension = ".jpg";
            System.out.println("page: " + page + ", " + originalFileName);
            BufferedImage image = ImageIO.read(new File(folder + originalFileName + extension));

            // BufferedImage grayscaleImage = makeGrayscale(image);
            BufferedImage grayscaleImage = detectEdges(image);
            ImageIO.write(grayscaleImage, "jpg", new File(folder + originalFileName + " - grayscale" + extension));
            // inspectColor();

            List<Row> rowList = splitIntoRows(grayscaleImage);
            rowList = mergeRows(rowList);
            splitRowsIntoCells(grayscaleImage, rowList);
            mergeCells(rowList);
            createChildImages(originalFileName, image, grayscaleImage, rowList, extension);
        }
    }

    private BufferedImage makeGrayscale(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        BufferedImage grayscaleImage = new BufferedImage(width, height, image.getType());
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = image.getRGB(x, y);
                int red = (rgb >> 16) & 0xFF;
                int green = (rgb >> 8) & 0xFF;
                int blue = rgb & 0xFF;

                // Calculate the grayscale value using the standard formula
                // This is a weighted average that takes into account human perception
                int gray = (int) (0.299 * red + 0.587 * green + 0.114 * blue);

                // Create a new RGB value with the same gray value for all three components
                int grayRgb = (gray << 16) | (gray << 8) | gray;

                // Set the pixel in the grayscale image.
                grayscaleImage.setRGB(x, y, grayRgb);
            }
        }
        return grayscaleImage;
    }

    // The grayscale image may have holes
    private BufferedImage detectEdgesOld(BufferedImage inputImage) {
        // https://en.wikipedia.org/wiki/Canny_edge_detector
        // https://github.com/rstreet85/JCanny
        double CANNY_THRESHOLD_RATIO = .2; // Suggested range .2 - .4
        int CANNY_STD_DEV = 1;             // Range 1-3
        return JCanny.CannyEdges(inputImage, CANNY_STD_DEV, CANNY_THRESHOLD_RATIO);
    }

    // Code from Google Gemini
    private BufferedImage detectEdges(BufferedImage inputImage) {
        CannyEdgeDetector detector = new CannyEdgeDetector();
        detector.detectEdges(inputImage, sigma, lowThreshold, highThreshold);
        return detector.getEdgesImage();
    }

    private List<Row> splitIntoRows(BufferedImage grayscaleImage) {
        List<Row> rowList = new ArrayList<>();
        int startY = 0;
        int grayscaleHeight = grayscaleImage.getHeight();
        int grayscaleWidth = grayscaleImage.getWidth();

        while (startY < grayscaleHeight) {
            while (startY < grayscaleHeight && isBackgroundHorizontal(startY, 0, grayscaleWidth, grayscaleImage)) {
                startY++;
            }
            if (startY == grayscaleHeight) {
                break;
            }

            int endY = startY + 1;
            while (endY < grayscaleHeight && !isBackgroundHorizontal(endY, 0, grayscaleWidth, grayscaleImage)) {
                endY++;
            }

            // System.out.println(startY + " -> " + endY + " / " + grayscaleHeight);
            Row row = new Row();
            row.startY = startY;
            row.endY = endY;
            rowList.add(row);

            startY = endY;
        }
        return rowList;
    }

    private void inspectColor(BufferedImage image) {
        Set<Integer> colorValueSet = new HashSet<>();
        for (int x = 0; x < image.getWidth(); x++) {
            for (int y = 0; y < image.getHeight(); y++) {
                int rgb = image.getRGB(x, y);
                colorValueSet.add(rgb);
            }
        }

        // -16777216: black, background
        // -1: white
        for (Integer n : colorValueSet) {
            System.out.println(n);
        }
        System.out.println(image.getRGB(1, 1));
    }

    private boolean isBackgroundHorizontal(int y, int fromX, int toX, BufferedImage grayscaleImage) {
        for (int x = fromX; x < toX; x++) {
            int rgb;
            try {
                rgb = grayscaleImage.getRGB(x, y);
            } catch (ArrayIndexOutOfBoundsException ex) {
                System.out.println("(" + x + ", " + y + ") in " + grayscaleImage.getWidth() + "x" + grayscaleImage.getHeight());
                // ex.printStackTrace();
                throw ex;
            }

            if (rgb != BACKGROUND_COLOR) {
                return false;
            }
        }
        return true;
    }

    private boolean isBackgroundVertical(int x, int startY, int endY, BufferedImage grayscaleImage) {
        for (int y = startY; y < endY; y++) {
            int rgb = grayscaleImage.getRGB(x, y);
            if (rgb != BACKGROUND_COLOR) {
                return false;
            }
        }
        return true;
    }

    private List<Row> mergeRows(List<Row> rowList) {
        // Merge continuous small rows
        for (int i = rowList.size() - 1; i > 0; i--) {
            Row nextRow = rowList.get(i);
            Row previousRow = rowList.get(i - 1);
            boolean bothAreSmall = nextRow.endY - nextRow.startY < minimumHeight
                && previousRow.endY - previousRow.startY < minimumHeight;
            boolean previousIsSmallAndConnectToNext = previousRow.endY - previousRow.startY < minimumHeight
                && nextRow.startY - previousRow.endY < continuousRowsGap;
            if (bothAreSmall || previousIsSmallAndConnectToNext) {
                previousRow.endY = nextRow.endY;
                rowList.remove(i);
            }
        }

        Row firstRow = rowList.get(0);
        List<Row> mergedRowList = new ArrayList<>();
        mergedRowList.add(firstRow);
        boolean forceMergeWithPrevious = firstRow.endY - firstRow.startY < minimumHeight;

        for (int i = 1; i < rowList.size(); i++) {
            Row row = rowList.get(i);
            if (forceMergeWithPrevious) {
                Row previousRow = mergedRowList.get(mergedRowList.size() - 1);
                previousRow.endY = row.endY;
                forceMergeWithPrevious = previousRow.endY - previousRow.startY < minimumHeight;
            } else if (row.endY - row.startY < minimumHeight) {
                Row previousRow = mergedRowList.get(mergedRowList.size() - 1);
                previousRow.endY = row.endY;
            } else {
                mergedRowList.add(row);
            }
        }

        return mergedRowList;
    }

    private void splitRowsIntoCells(BufferedImage grayscaleImage, List<Row> rowList) {
        int grayscaleWidth = grayscaleImage.getWidth();
        for (Row row : rowList) {
            int startY = row.startY;
            int endY = row.endY;

            int startX = 0;
            while (startX < grayscaleWidth) {
                while (startX < grayscaleWidth && isBackgroundVertical(startX, startY, endY, grayscaleImage)) {
                    startX++;
                }
                if (startX == grayscaleWidth) {
                    break;
                }

                int endX = startX + 1;
                while (endX < grayscaleWidth && !isBackgroundVertical(endX, startY, endY, grayscaleImage)) {
                    endX++;
                }

                // System.out.println(startX + " -> " + endX + " / " + grayscaleWidth);
                Cell cell = new Cell(startX, endX);
                row.cellList.add(cell);

                startX = endX;
            }

            // row.cellList.add(new Cell(0, grayscaleWidth));
        }
    }

    private void mergeCells(List<Row> rowList) {
        for (Row row : rowList) {
            List<Cell> cellList = row.cellList;
            Cell firstCell = cellList.get(0);
            List<Cell> mergedCellList = new ArrayList<>();
            mergedCellList.add(firstCell);
            boolean forceMergeWithPrevious = firstCell.endX - firstCell.startX < minimumWidth;

            for (int i = 1; i < cellList.size(); i++) {
                Cell cell = cellList.get(i);
                if (forceMergeWithPrevious) {
                    Cell previousCell = mergedCellList.get(mergedCellList.size() - 1);
                    previousCell.endX = cell.endX;
                    forceMergeWithPrevious = previousCell.endX - previousCell.startX < minimumWidth;
                } else if (cell.endX - cell.startX < minimumWidth) {
                    Cell previousCell = mergedCellList.get(mergedCellList.size() - 1);
                    previousCell.endX = cell.endX;
                } else {
                    mergedCellList.add(cell);
                }
            }

            row.cellList = mergedCellList;
        }
    }

    private void createChildImages(String originalFileName, BufferedImage image, BufferedImage grayscaleImage, List<Row> rowList, String extension) throws Exception {
        int cuttingFrame = 0;
        for (int i = 0; i < rowList.size(); i++) {
            Row row = rowList.get(i);
            List<Cell> cellList = row.cellList;
            for (int j = 0; j < cellList.size(); j++) {
                Cell cell = cellList.get(j);
                int startY = row.startY;
                int endY = row.endY;
                int startX = cell.startX;
                int endX = cell.endX;

                while (startY < endY && isBackgroundHorizontal(startY, startX, endX, grayscaleImage)) {
                    startY++;
                }

                while (endY > startY && isBackgroundHorizontal(endY - 1, startX, endX, grayscaleImage)) {
                    endY--;
                }

                int childHeight = endY - startY;
                int childWidth = endX - startX;
                BufferedImage childImage = image.getSubimage(
                    startX + offset + cuttingFrame,
                    startY + offset + cuttingFrame,
                    childWidth - cuttingFrame * 2,
                    childHeight - cuttingFrame * 2
                );
                String newFileName = String.format("%s - %s.%s", originalFileName, String.format("%02d", i + 1), String.format("%02d", j + 1));
                ImageIO.write(childImage, "jpg", new File(folder + newFileName + extension));
            }
        }
    }
}