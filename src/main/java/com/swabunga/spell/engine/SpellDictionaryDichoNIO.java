/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.swabunga.spell.engine;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author manu
 */
public class SpellDictionaryDichoNIO extends SpellDictionaryDichoDisk {


    private ByteBuffer buffer;

    /**
     * Found words
     */
    public SpellDictionaryDichoNIO(File wordList) throws FileNotFoundException, IOException {
        super(wordList);
    }

    public SpellDictionaryDichoNIO(File wordList, String encoding) throws FileNotFoundException, IOException {
        super(wordList, encoding);
    }

    public SpellDictionaryDichoNIO(File wordList, File phonetic) throws FileNotFoundException, IOException {
        super(wordList, phonetic);
    }

    public SpellDictionaryDichoNIO(File wordList, File phonetic, String encoding) throws FileNotFoundException, IOException {
        super(wordList, phonetic, encoding);

        FileChannel roChannel = dictFile.getChannel();
        buffer = roChannel.map(FileChannel.MapMode.READ_ONLY, 0, (int) roChannel.size());
    }

    private void dichoFind(String code, int p1, int p2, ArrayList<String> wordList) throws IOException {
        //System.out.println("dichoFind("+code+","+p1+","+p2+")");

        int pm = (int) ((p1 + p2) / 2);
        buffer.position(pm);
        nextLine();

        // Beginning of the current line
        pm = buffer.position();
        String line = readLine();
        int pm2 = buffer.position();

        if (pm2 >= p2) {
            seqFind(code, p1, p2, wordList);
            return;
        }

        int istar = line.indexOf(SEP);
        if (istar == -1) {
            throw new IOException("bad format: no * !");
        }

        int comp = code.compareTo(line.substring(0, istar));

        if (comp < 0) {
            dichoFind(code, p1, pm - 1, wordList);
        } else if (comp > 0) {
            dichoFind(code, pm2, p2, wordList);
        } else {
            dichoFind(code, p1, pm - 1, wordList);
            wordList.add(line.substring(istar + 1));
            dichoFind(code, pm2, p2, wordList);
        }
    }

    private void seqFind(String code, int p1, int p2, ArrayList<String> wordList) throws IOException {
        buffer.position(p1);

        while (buffer.position() < p2) {
            String line = readLine();

            int istar = line.indexOf(SEP);
            if (istar == -1) {
                throw new IOException("bad format: no " + SEP + " !");
            }

            if (line.substring(0, istar).equals(code)) {
                wordList.add(line.substring(istar + 1));
            }
        }
    }

    private void nextLine() throws IOException {
        while (buffer.position() < buffer.limit()) {
            byte b = buffer.get();
            //if ( b == '\n' || b == '\r' ) break ;
            if (b == '\n') {
                break;
            }
        }
    }

    private String readLine() throws IOException {
        byte[] buf = new byte[255];
        int i = 0;

        while (buffer.position() < buffer.limit() && i < 255) {
            byte b = buffer.get();
            //if ( b == '\n' || b == '\r' ) break ;
            if (b == '\n') {
                break;
            }
            buf[i++] = b;
        }

        if (i == 0) {
            return "";
        }
        return (encoding == null) ? new String(buf, 0, i) : new String(buf, 0, i, encoding);
    }

    /**
     * Returns a list of strings (words) for the code.
     */
    @Override
    public synchronized List getWords(String code) {
        ArrayList<String> wordList = new ArrayList();

        try {
            buffer.rewind();
            dichoFind(code, 0, buffer.capacity() - 1, wordList);
        } 
        catch (IOException ex) {
            System.err.println("IOException: " + ex.getMessage());
        }

        return wordList;
    }

}
