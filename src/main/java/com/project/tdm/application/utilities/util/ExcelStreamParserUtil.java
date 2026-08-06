package com.project.tdm.application.utilities.util;

import com.project.tdm.application.entity.PostcodeEntity;
import org.apache.poi.util.IOUtils;
import org.springframework.stereotype.Component;

import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.xssf.eventusermodel.XSSFReader;
import org.apache.poi.xssf.eventusermodel.XSSFSheetXMLHandler;
import org.apache.poi.xssf.model.SharedStringsTable;
import org.apache.poi.xssf.model.StylesTable;
import org.springframework.stereotype.Component;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

import java.io.InputStream;
import java.util.function.Consumer;

@Component
public class ExcelStreamParserUtil {

    static {
        IOUtils.setByteArrayMaxOverride(300 * 1024 * 1024);
    }

    public void parseExcelFile(InputStream inputStream, Consumer<PostcodeEntity> rowConsumer) throws Exception {
        try (OPCPackage pkg = OPCPackage.open(inputStream)) {
            XSSFReader reader = new XSSFReader(pkg);
            SharedStringsTable sst = (SharedStringsTable) reader.getSharedStringsTable();
            StylesTable styles = reader.getStylesTable();

            InternalSheetHandler sheetHandler = new InternalSheetHandler(rowConsumer);

            XMLReader parser = XMLReaderFactory.createXMLReader();
            parser.setContentHandler(new XSSFSheetXMLHandler(styles, sst, sheetHandler, false));

            InputStream sheetStream = reader.getSheetsData().next();
            parser.parse(new InputSource(sheetStream));
            sheetStream.close();
        }
    }

    private static class InternalSheetHandler implements XSSFSheetXMLHandler.SheetContentsHandler {
        private final Consumer<PostcodeEntity> consumer;
        private boolean isHeader = true;
        private String currentPostcode;
        private Double currentLat;
        private Double currentLng;

        public InternalSheetHandler(Consumer<PostcodeEntity> consumer) {
            this.consumer = consumer;
        }

        @Override
        public void startRow(int rowNum) {
            currentPostcode = null;
            currentLat = null;
            currentLng = null;
            if (rowNum > 0) isHeader = false;
        }

        @Override
        public void endRow(int rowNum) {
            if (!isHeader && currentPostcode != null && currentLat != null && currentLng != null) {
                consumer.accept(new PostcodeEntity(currentPostcode, currentLat, currentLng));
            }
        }

        @Override
        public void cell(String cellReference, String formattedValue, org.apache.poi.xssf.usermodel.XSSFComment comment) {
            if (isHeader) return;

            if (cellReference.startsWith("A")) currentPostcode = formattedValue;
            else if (cellReference.startsWith("B")) currentLat = Double.parseDouble(formattedValue);
            else if (cellReference.startsWith("C")) currentLng = Double.parseDouble(formattedValue);
        }
    }
}
