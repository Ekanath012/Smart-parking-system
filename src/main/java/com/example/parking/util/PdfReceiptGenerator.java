package com.example.parking.util;

import com.example.parking.model.Bill;
import com.example.parking.model.Booking;
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;

/**
 * Utility class for generating professional PDF Payment Receipts with embedded QR Code using LibrePDF/OpenPDF.
 */
public class PdfReceiptGenerator {

    public static byte[] generateBillPdf(Booking booking, Bill bill, byte[] qrCodeBytes) throws DocumentException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 36, 36, 36, 36);
        PdfWriter.getInstance(document, out);

        document.open();

        // Fonts
        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 22, new Color(37, 99, 235));
        Font subtitleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, Color.GRAY);
        Font bodyBold = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.DARK_GRAY);
        Font bodyRegular = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.DARK_GRAY);

        // Header Title & Logo
        Paragraph title = new Paragraph("SMART PARKING SYSTEM", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        document.add(title);

        Paragraph subtitle = new Paragraph("OFFICIAL PAYMENT RECEIPT", subtitleFont);
        subtitle.setAlignment(Element.ALIGN_CENTER);
        subtitle.setSpacingAfter(15);
        document.add(subtitle);

        // Embed QR Code in PDF if provided
        if (qrCodeBytes != null && qrCodeBytes.length > 0) {
            try {
                Image qrImage = Image.getInstance(qrCodeBytes);
                qrImage.scaleToFit(100, 100);
                qrImage.setAlignment(Element.ALIGN_CENTER);
                document.add(qrImage);
            } catch (Exception e) {
                // Ignore QR embedding errors
            }
        }

        // Booking Info Table
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setSpacingBefore(15);
        table.setSpacingAfter(20);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm");

        addTableRow(table, "Booking Reference:", booking.getBookingReference(), bodyBold, bodyRegular);
        addTableRow(table, "Driver Name:", booking.getUser().getName(), bodyBold, bodyRegular);
        addTableRow(table, "Vehicle Number:", booking.getVehicleNumber(), bodyBold, bodyRegular);
        addTableRow(table, "Parking Slot:", booking.getParkingSlot().getSlotNumber() + " (" + booking.getParkingSlot().getSlotType() + ")", bodyBold, bodyRegular);
        addTableRow(table, "Floor Level:", booking.getParkingSlot().getFloorLevel(), bodyBold, bodyRegular);
        addTableRow(table, "Check-In Time:", booking.getEntryTime().format(formatter), bodyBold, bodyRegular);
        addTableRow(table, "Check-Out Time:", booking.getExitTime() != null ? booking.getExitTime().format(formatter) : "N/A", bodyBold, bodyRegular);
        addTableRow(table, "Duration Billed:", bill.getTotalHours() + " Hours", bodyBold, bodyRegular);
        addTableRow(table, "Hourly Rate:", "₹" + bill.getHourlyRate() + "/hr", bodyBold, bodyRegular);
        addTableRow(table, "Total Amount Paid:", "₹" + bill.getTotalAmount(), FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, new Color(34, 197, 94)), FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, new Color(34, 197, 94)));

        document.add(table);

        // Footer
        Paragraph footer = new Paragraph("Thank you for using Smart Parking Management System!\nThis is an official computer-generated receipt.", FontFactory.getFont(FontFactory.HELVETICA, 9, Font.ITALIC, Color.GRAY));
        footer.setAlignment(Element.ALIGN_CENTER);
        document.add(footer);

        document.close();
        return out.toByteArray();
    }

    private static void addTableRow(PdfPTable table, String label, String value, Font labelFont, Font valueFont) {
        PdfPCell cell1 = new PdfPCell(new Phrase(label, labelFont));
        cell1.setPadding(8);
        cell1.setBorderColor(new Color(226, 232, 240));

        PdfPCell cell2 = new PdfPCell(new Phrase(value, valueFont));
        cell2.setPadding(8);
        cell2.setHorizontalAlignment(Element.ALIGN_RIGHT);
        cell2.setBorderColor(new Color(226, 232, 240));

        table.addCell(cell1);
        table.addCell(cell2);
    }
}
