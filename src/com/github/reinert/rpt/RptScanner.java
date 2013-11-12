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
            throw new Exception("O caminho do arquivo rpt deve ser informado como primeiro argumento. Em seguida, para cada coluna do rpt deve ser informado 0 se a coluna for numero, 1 se for texto e 2 se for boleano. Por fim, opcionalmente podem ser informados os charsets de entrada e saida com as variaveis de contexto 'charset.in' e 'charset.out' e o caracter delimitador de colunas com a varaivel 'delimiter'. Tambem e possivel informar as strings de entrada para true (boolean.true) e false (boolean.false) no caso dos campos boleanos");
        }

        String inCharset = System.getProperty("charset.in", "UTF-8");
        String outCharset = System.getProperty("charset.out", "UTF-8");
        String outDelimiter = System.getProperty("delimiter", ",");
        String inTrue = System.getProperty("true.in", "true");
        String inFalse = System.getProperty("false.in", "false");
        String outTrue = System.getProperty("true.out", "true");
        String outFalse = System.getProperty("false.out", "false");
        String[] nullStrings = System.getProperty("null.in", "NULL").split(",");
        String outNull = System.getProperty("null.out", "NULL");

        String notNullColsParam = System.getProperty("notnull.cols");
        int[] notnullCols = null;
        if (notNullColsParam != null) {
            String[] notnullColsStr = notNullColsParam.split(",");
            notnullCols = new int[notnullColsStr.length];
            for (int i = 0; i < notnullColsStr.length; i++) {
                String notnullCol = notnullColsStr[i];
                notnullCols[i] = Integer.parseInt(notnullCol);
            }
        }

        BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(args[0]), inCharset));

        String headerLine = reader.readLine();

        String[] limitsArray = reader.readLine().split(" ");

        if ((limitsArray.length + 1) != (args.length)) {
            throw new Exception("O rpt contem " + limitsArray.length + " colunas e vc informou " + (args.length - 1)
                    + ". Para cada coluna no arquivo rpt vc deve informar 0 (numero), 1 (texto), ou 2 (boleano).");
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

            // Trata todas as colunas exceto a ultima
            for (int i = 0; i < limits.length-1; i++) {
                int[] limit = limits[i];
                stringBuilder.append(delimiter);
                final String value = line.substring(limit[0], limit[1]).trim();
                appendValue(outNull ,nullStrings, notnullCols, inTrue, inFalse, outTrue, outFalse, stringBuilder, aspas, i, limit[2], value);
                delimiter = outDelimiter;
            }

            // Trata a ultima coluna
            stringBuilder.append(delimiter);
            final int[] limit = limits[limits.length - 1];
            appendValue(outNull ,nullStrings, notnullCols, inTrue, inFalse, outTrue, outFalse, stringBuilder, aspas, limits.length - 1, limit[2],
                    line.substring(limit[0]).trim());

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

    private static void appendValue(String outNull, String[] nullStrings, int[] notNullCols, String inTrue, String inFalse,
                                    String outTrue, String outFalse, StringBuilder stringBuilder, String aspas,
                                    int col, int colType, String value) throws Exception {

        boolean isNull = false;
        boolean isNotNullCol = false;
        if (notNullCols != null) {
            for (int i = 0; i < notNullCols.length; i++) {
                if (col == notNullCols[i]) {
                    isNotNullCol = true;
                    break;
                }
            }
        }
        if (!isNotNullCol) {
            for (int i = 0; i < nullStrings.length; i++) {
                if (nullStrings[i].equalsIgnoreCase(value)) {
                    isNull = true;
                    break;
                }
            }
        }
        if (isNull) {
            stringBuilder.append(outNull);
        } else if (colType == 1) {
            String str = StringEscapeUtils.escapeCsv(value);
            stringBuilder.append(aspas);
            if (str.startsWith("\"") && str.endsWith("\"")) str = str.substring(1, str.length()-1);
            stringBuilder.append(str);
            stringBuilder.append(aspas);
        } else if (colType == 2) {
            if (inTrue.equals(value)) {
                stringBuilder.append(outTrue);
            } else if (inFalse.equals(inFalse)) {
                stringBuilder.append(outFalse);
            } else {
                throw new Exception("Nao foi possivel ler as entradas da coluna " + col + " (boleana). Configure os valores de entrada boleanos atraves das variaveis de contexto 'boolean.true' e 'boolean.false'.");
            }
        } else {
            stringBuilder.append(StringEscapeUtils.escapeCsv(value));
        }
    }
}
