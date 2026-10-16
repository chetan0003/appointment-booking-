package com.jfl.appointment.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

@Service
public class QrCodeGeneratorService {


    private static final Color QR_GREEN =
            new Color(71, 118, 74);

    private static final Color WHITE =
            Color.WHITE;

    private static final int QUIET_ZONE = 4;

    // Logo file - drop your PNG directly under src/main/resources so it sits
    // at classpath root. Change this if your filename differs.
    private static final String LOGO_CLASSPATH = "/holamd-new-logo.png";

    // Logo circle diameter as a fraction of the QR's smaller dimension.
    // ERROR_CORRECTION_LEVEL.H tolerates ~30% of the code being obscured -
    // staying at ~22% here leaves real margin rather than cutting it close.
    private static final double LOGO_SIZE_RATIO = 0.22;

    // Thickness of the green ring around the white logo circle, as seen in
    // the reference image.
    private static final double LOGO_RING_RATIO = 0.015;

    public byte[] generateQrCode(
            String content,
            int width,
            int height
    ) {

        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException(
                    "QR content cannot be empty"
            );
        }

//        if (width < 200 || height < 200) {
//            throw new IllegalArgumentException(
//                    "QR dimensions must be at least 200x200"
//            );
//        }

        try {

            Map<EncodeHintType, Object> hints =
                    new HashMap<>();

            hints.put(
                    EncodeHintType.ERROR_CORRECTION,
                    ErrorCorrectionLevel.H
            );

            hints.put(
                    EncodeHintType.MARGIN,
                    QUIET_ZONE
            );

            // Generate QR at its natural module resolution
            BitMatrix matrix =
                    new MultiFormatWriter().encode(
                            content,
                            BarcodeFormat.QR_CODE,
                            1,
                            1,
                            hints
                    );

            int modulesX = matrix.getWidth();
            int modulesY = matrix.getHeight();

            BufferedImage image =
                    new BufferedImage(
                            width,
                            height,
                            BufferedImage.TYPE_INT_RGB
                    );

            Graphics2D g = image.createGraphics();

            g.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON
            );

            g.setRenderingHint(
                    RenderingHints.KEY_RENDERING,
                    RenderingHints.VALUE_RENDER_QUALITY
            );

            // White background
            g.setColor(WHITE);
            g.fillRect(0, 0, width, height);

            /*
             * Keep QR square and centered.
             */
            double cellSize = Math.min(
                    (double) width / modulesX,
                    (double) height / modulesY
            );

            double qrWidth = modulesX * cellSize;
            double qrHeight = modulesY * cellSize;

            double offsetX = (width - qrWidth) / 2;
            double offsetY = (height - qrHeight) / 2;

            /*
             * Draw circular QR modules.
             */
            g.setColor(QR_GREEN);

            for (int y = QUIET_ZONE;
                 y < modulesY - QUIET_ZONE;
                 y++) {

                for (int x = QUIET_ZONE;
                     x < modulesX - QUIET_ZONE;
                     x++) {

                    if (!matrix.get(x, y)) {
                        continue;
                    }

                    if (isFinderArea(
                            x,
                            y,
                            modulesX,
                            modulesY
                    )) {
                        continue;
                    }

                    double dotSize = cellSize * 0.86;

                    double px =
                            offsetX
                                    + x * cellSize
                                    + (cellSize - dotSize) / 2;

                    double py =
                            offsetY
                                    + y * cellSize
                                    + (cellSize - dotSize) / 2;

                    g.fill(
                            new Ellipse2D.Double(
                                    px,
                                    py,
                                    dotSize,
                                    dotSize
                            )
                    );
                }
            }

            /*
             * Draw three custom finder patterns.
             */

            drawFinderPattern(
                    g,
                    QUIET_ZONE,
                    QUIET_ZONE,
                    cellSize,
                    offsetX,
                    offsetY
            );

            drawFinderPattern(
                    g,
                    modulesX - QUIET_ZONE - 7,
                    QUIET_ZONE,
                    cellSize,
                    offsetX,
                    offsetY
            );

            drawFinderPattern(
                    g,
                    QUIET_ZONE,
                    modulesY - QUIET_ZONE - 7,
                    cellSize,
                    offsetX,
                    offsetY
            );

            /*
             * Overlay the center logo LAST, on top of everything else drawn
             * so far - this is what was missing. Relies on
             * ErrorCorrectionLevel.H above to keep the code scannable despite
             * the modules this covers.
             */
            drawCenterLogo(g, width, height);

            g.dispose();

            ByteArrayOutputStream output =
                    new ByteArrayOutputStream();

            ImageIO.write(
                    image,
                    "PNG",
                    output
            );

            return output.toByteArray();

        } catch (Exception e) {

            throw new IllegalStateException(
                    "Unable to generate branded QR code",
                    e
            );
        }
    }

    /**
     * Draws a white circle with a thin green ring in the center of the QR
     * code, then the logo image scaled to fit inside it - matching the
     * reference branding (green flower mark + "Hola MD" wordmark).
     * If the logo file can't be loaded, logs a warning and skips the overlay
     * rather than failing the whole QR generation - a missing logo image
     * shouldn't take down appointment booking.
     */
    private void drawCenterLogo(Graphics2D g, int width, int height) {

        double diameter = LOGO_SIZE_RATIO * Math.min(width, height);
        double ringThickness = LOGO_RING_RATIO * Math.min(width, height);
        double centerX = width / 2.0;
        double centerY = height / 2.0;

        // Outer green ring
        g.setColor(QR_GREEN);
        g.fill(new Ellipse2D.Double(
                centerX - diameter / 2 - ringThickness,
                centerY - diameter / 2 - ringThickness,
                diameter + ringThickness * 2,
                diameter + ringThickness * 2
        ));

        // White inner circle the logo sits on
        g.setColor(WHITE);
        g.fill(new Ellipse2D.Double(
                centerX - diameter / 2,
                centerY - diameter / 2,
                diameter,
                diameter
        ));

        BufferedImage logo = loadLogo();
        if (logo == null) {
            return;
        }

        // Fit the logo inside ~82% of the white circle, preserving aspect
        // ratio, so there's a little breathing room before the green ring.
        double maxLogoSize = diameter * 0.82;
        double scale = Math.min(
                maxLogoSize / logo.getWidth(),
                maxLogoSize / logo.getHeight()
        );
        int logoW = (int) Math.round(logo.getWidth() * scale);
        int logoH = (int) Math.round(logo.getHeight() * scale);

        int logoX = (int) Math.round(centerX - logoW / 2.0);
        int logoY = (int) Math.round(centerY - logoH / 2.0);

        g.drawImage(logo, logoX, logoY, logoW, logoH, null);
    }

    private BufferedImage loadLogo() {
        try (InputStream in = getClass().getResourceAsStream(LOGO_CLASSPATH)) {
            if (in == null) {
                System.err.println("QR logo not found on classpath at " + LOGO_CLASSPATH
                        + " - generating QR without the center logo.");
                return null;
            }
            return ImageIO.read(in);
        } catch (Exception e) {
            System.err.println("Failed to load QR logo from " + LOGO_CLASSPATH
                    + " - generating QR without the center logo: " + e.getMessage());
            return null;
        }
    }

    /**
     * Identifies the three QR finder pattern regions.
     */
    private boolean isFinderArea(
            int x,
            int y,
            int modulesX,
            int modulesY
    ) {

        int start = QUIET_ZONE;
        int end = QUIET_ZONE + 7;

        boolean topLeft =
                x >= start && x < end
                        && y >= start && y < end;

        boolean topRight =
                x >= modulesX - end
                        && x < modulesX - start
                        && y >= start && y < end;

        boolean bottomLeft =
                x >= start && x < end
                        && y >= modulesY - end
                        && y < modulesY - start;

        return topLeft || topRight || bottomLeft;
    }

    /**
     * Draws a rounded finder pattern:
     * Green outer shape
     * White inner gap
     * Green center
     */
    private void drawFinderPattern(
            Graphics2D g,
            int moduleX,
            int moduleY,
            double cellSize,
            double offsetX,
            double offsetY
    ) {

        int x = (int) Math.round(
                offsetX + moduleX * cellSize
        );

        int y = (int) Math.round(
                offsetY + moduleY * cellSize
        );

        int outer = (int) Math.round(7 * cellSize);
        int middle = (int) Math.round(5 * cellSize);
        int inner = (int) Math.round(3 * cellSize);

        // Outer green rounded square
        g.setColor(QR_GREEN);

        g.fillRoundRect(
                x,
                y,
                outer,
                outer,
                outer / 2,
                outer / 2
        );

        // White inner area
        g.setColor(WHITE);

        g.fillRoundRect(
                x + (outer - middle) / 2,
                y + (outer - middle) / 2,
                middle,
                middle,
                middle / 3,
                middle / 3
        );

        // Green center
        g.setColor(QR_GREEN);

        g.fillRoundRect(
                x + (outer - inner) / 2,
                y + (outer - inner) / 2,
                inner,
                inner,
                inner / 3,
                inner / 3
        );
    }

}