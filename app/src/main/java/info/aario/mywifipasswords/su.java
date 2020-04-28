package info.aario.mywifipasswords;

import android.util.Log;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;

/**
 * Created by blinkingtwelve on 2020-04-28.
 */


class SimpleSuexec {

    public final int retval;
    public final String stdout;
    public final String stderr;


    private String drain_textstream(InputStream input) throws IOException {
        StringBuilder textbuf = new StringBuilder();
        BufferedReader in = new BufferedReader(new InputStreamReader(input, Charset.forName("utf-8")));
        String line;
        while ((line = in.readLine()) != null) {
            textbuf.append(line);
            textbuf.append('\n');
        }
        return textbuf.toString();
    }


    public SimpleSuexec(String[] args) throws InterruptedException, IOException {
        /*
        This is not a generic Android su wrapper.
            Assumptions:
            1. Process runs to completion without blocking on reading stdin
            2. Caller wants and expects stderr / stdout as a string
            3. Platform's su is a standard su.
        */

        String[] su_args = new String[]{"su", "root", "--"};
        String[] cmd = new String[su_args.length + args.length];
        for (int i = 0; i < cmd.length; i++) {
            Boolean nextarray = (i >= su_args.length);
            String[] src_array = nextarray ? args : su_args;
            int src_array_ix = nextarray ? i - su_args.length : i;
            cmd[i] = src_array[src_array_ix];
        }

        Process su_proc = Runtime.getRuntime().exec(cmd);
        String stderr = null;
        try {
            stderr = drain_textstream(su_proc.getErrorStream());
        } catch (IOException e) {
            Log.e("SUEXEC", "Error while draining stderr:\n" + e.getStackTrace());
        }

        String stdout = null;
        try {
            stdout = drain_textstream(su_proc.getInputStream());
        } catch (IOException e) {
            Log.e("SUEXEC", "Error while draining stdout:\n" + e.getStackTrace());
        }

        this.stderr = stderr;
        this.stdout = stdout;
        this.retval = su_proc.waitFor();
    }


    public static boolean have_su() {
        String[] su_test_args = {"su", "root", "--", "id", "-u"};
        try {
            Process su_proc = Runtime.getRuntime().exec(su_test_args);
            if ('0' == new DataInputStream(su_proc.getInputStream()).readChar() && 0 != su_proc.waitFor())
                return true;
            return false;
        } catch (IOException | InterruptedException e) {
            Log.e("ROOTCHECK", "Root access unavailable:\n" + e.getStackTrace());
            return false;
        }

    }
}