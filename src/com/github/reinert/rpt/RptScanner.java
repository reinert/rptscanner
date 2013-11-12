package com.github.reinert.rpt;

import org.apache.commons.lang3.StringEscapeUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;

/**
 * @author Danilo Reinert
 */
public class RptScanner {

    public static final String LINE_BREAK = System.getProperty("line.separator");

    public static void main(String[] args) throws Exception {

        if (args.length == 1) {
            throw new Exception("O caminho do arquivo rpt deve ser informado como primeiro argumento. Em seguida, para cada coluna do rpt deve ser informado 0 se a coluna for numero e 1 se for texto. Por fim, opcionalmente podem ser informados os charsets de entrada e saida com as variaveis de contexto 'charset.in' e 'charset.out' e o caracter delimitador de colunas com a varaivel 'delimiter'.");
        }

        String inCharset = System.getProperty("charset.in", "UTF-8");
        String outCharset = System.getProperty("charset.out", "UTF-8");
        String outDelimiter = System.getProperty("delimiter", ",");

        BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(args[0]), inCharset));

        String headerLine = reader.readLine();

        String[] limitsArray = reader.readLine().split(" ");

        if ((limitsArray.length + 1) != (args.length)) {
            throw new Exception("O rpt contem " + limitsArray.length + " colunas e vc informou " + (args.length - 1)
                    + ". Para cada coluna no arquivo rpt vc deve informar 0 (numero) ou 1 (texto).");
        }

        int[][] limits = new int[limitsArray.length][3];

        int lastCol = -1;
        for (int i = 0; i < limitsArray.length; i++) {
            String s = limitsArray[i];
            limits[i][0] = lastCol + 1;
            limits[i][1] = limits[i][0] + s.length();
            limits[i][2] = Integer.valueOf(args[i+1]);
            lastCol = limits[i][1];
        }

        StringBuilder stringBuilder = new StringBuilder();

        String aspas = "\"";
        String delimiter = "";

        // mount header
        for (int i = 0; i < limits.length-1; i++) {
            int[] limit = limits[i];
            stringBuilder.append(delimiter);
            final String value = headerLine.substring(limit[0], limit[1]).trim();
            stringBuilder.append(aspas);
            stringBuilder.append(StringEscapeUtils.escapeCsv(value));
            stringBuilder.append(aspas);
            delimiter = outDelimiter;
        }
        stringBuilder.append(LINE_BREAK);

        String line;
        while ((line = reader.readLine()) != null) {
            if (line.trim().isEmpty()) break;

            delimiter = "";

            for (int i = 0; i < limits.length-1; i++) {
                int[] limit = limits[i];
                stringBuilder.append(delimiter);
                final String value = line.substring(limit[0], limit[1]).trim();
                if (limit[2] == 1) {
                    stringBuilder.append(aspas);
                    stringBuilder.append(StringEscapeUtils.escapeCsv(value));
                    stringBuilder.append(aspas);
                } else {
                    stringBuilder.append(StringEscapeUtils.escapeCsv(value));
                }
                delimiter = outDelimiter;
            }
            stringBuilder.append(delimiter);
            stringBuilder.append(line.substring(limits[limits.length-1][0]).trim());

            stringBuilder.append(LINE_BREAK);
        }

        String outputPath = args[0].contains(File.separator) ? args[0].substring(0, args[0].lastIndexOf(File.separator)) + File.separator : "";
        outputPath += "result.csv";
        Writer out = new OutputStreamWriter(new FileOutputStream(outputPath), outCharset);
        try {
            out.write(stringBuilder.toString());
        }
        finally {
            out.close();
        }
    }
}
