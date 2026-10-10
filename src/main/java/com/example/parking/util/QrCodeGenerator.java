package com.example.parking.util;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;

/**
 * Utility class for generating ZXing QR Code images, PNG byte streams, and Base64 Data URIs.
 */
public class QrCodeGenerator {

    /**
     * Generate QR Code as raw PNG byte array for downloading.
     */
    public static byte[] generateQrCodeBytes(String text, int width, int height)
            throws WriterException, IOException {
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        BitMatrix bitMatrix = qrCodeWriter.encode(text, BarcodeFormat.QR_CODE, width, height);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(bitMatrix, "PNG", outputStream);
        return outputStream.toByteArray();
    }

    /**
     * Generate QR Code as Base64 encoded Data URI string for HTML rendering.
     */
    public static String generateQrCodeBase64(String text, int width, int height)
            throws WriterException, IOException {
        byte[] pngData = generateQrCodeBytes(text, width, height);
        return "data:image/png;base64," + Base64.getEncoder().encodeToString(pngData);
    }
}
