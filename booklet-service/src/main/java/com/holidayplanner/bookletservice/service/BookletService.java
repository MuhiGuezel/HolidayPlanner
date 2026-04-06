package com.holidayplanner.bookletservice.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

@Slf4j
@Service
public class BookletService {

    /**
     * Generates a PDF booklet for an organization.
     *
     * @param organizationName name of the organization
     * @param contactInfo      contact details of the org team
     * @param eventSummaries   list of event summaries (title + date + details)
     * @param sponsorNames     list of sponsor names
     * @return PDF as byte array
     */
    public byte[] generateBooklet(String organizationName,
                                  String contactInfo,
                                  List<String> eventSummaries,
                                  List<String> sponsorNames) throws IOException {

        try (PDDocument document = new PDDocument()) {

            // --- Page 1: Introduction ---
            PDPage introPage = new PDPage(PDRectangle.A4);
            document.addPage(introPage);

            try (PDPageContentStream cs = new PDPageContentStream(document, introPage)) {
                PDType1Font bold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
                PDType1Font regular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);

                cs.beginText();
                cs.setFont(bold, 24);
                cs.newLineAtOffset(60, 760);
                cs.showText("Holiday Planner – " + organizationName);

                cs.setFont(regular, 12);
                cs.newLineAtOffset(0, -40);
                cs.showText("Welcome to the Holiday Planner event booklet.");
                cs.newLineAtOffset(0, -20);
                cs.showText("Browse all events and book online at: https://holidayplanner.example.com");

                cs.newLineAtOffset(0, -40);
                cs.setFont(bold, 14);
                cs.showText("Contact Information");
                cs.setFont(regular, 12);
                cs.newLineAtOffset(0, -20);
                cs.showText(contactInfo);
                cs.endText();
            }

            // --- Page 2: Event Index ---
            PDPage eventPage = new PDPage(PDRectangle.A4);
            document.addPage(eventPage);

            try (PDPageContentStream cs = new PDPageContentStream(document, eventPage)) {
                PDType1Font bold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
                PDType1Font regular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);

                cs.beginText();
                cs.setFont(bold, 18);
                cs.newLineAtOffset(60, 760);
                cs.showText("Events");

                cs.setFont(regular, 11);
                float yPosition = 720;

                for (String eventSummary : eventSummaries) {
                    if (yPosition < 60) {
                        // TODO: add new page if content overflows
                        break;
                    }
                    cs.newLineAtOffset(0, -25);
                    cs.showText("• " + eventSummary);
                    yPosition -= 25;
                }
                cs.endText();
            }

            // --- Page 3: Sponsors ---
            PDPage sponsorPage = new PDPage(PDRectangle.A4);
            document.addPage(sponsorPage);

            try (PDPageContentStream cs = new PDPageContentStream(document, sponsorPage)) {
                PDType1Font bold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
                PDType1Font regular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);

                cs.beginText();
                cs.setFont(bold, 18);
                cs.newLineAtOffset(60, 760);
                cs.showText("Our Sponsors");

                cs.setFont(regular, 12);
                for (String sponsor : sponsorNames) {
                    cs.newLineAtOffset(0, -25);
                    cs.showText("• " + sponsor);
                }
                cs.endText();
            }

            // Write to byte array
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.save(out);
            log.info("Booklet generated for organization: {}", organizationName);
            return out.toByteArray();
        }
    }

    /**
     * Generates a participant list PDF for a caregiver.
     */
    public byte[] generateParticipantListPdf(String eventName, String termDate,
                                              List<String> participantNames) throws IOException {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            try (PDPageContentStream cs = new PDPageContentStream(document, page)) {
                PDType1Font bold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
                PDType1Font regular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);

                cs.beginText();
                cs.setFont(bold, 18);
                cs.newLineAtOffset(60, 760);
                cs.showText("Participant List");

                cs.setFont(regular, 13);
                cs.newLineAtOffset(0, -30);
                cs.showText("Event: " + eventName);
                cs.newLineAtOffset(0, -20);
                cs.showText("Date: " + termDate);

                cs.setFont(bold, 13);
                cs.newLineAtOffset(0, -35);
                cs.showText("Participants (" + participantNames.size() + "):");

                cs.setFont(regular, 12);
                int count = 1;
                for (String name : participantNames) {
                    cs.newLineAtOffset(0, -22);
                    cs.showText(count++ + ". " + name);
                }
                cs.endText();
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.save(out);
            return out.toByteArray();
        }
    }
}
