package info.aario.mywifipasswords;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.DataInputStream;
import android.util.Log;

import java.io.InputStreamReader;
import java.io.IOException;
/**
 * Created by aario on 3/14/17.
 */

public class ExecuteAsRootBase {
    public static boolean canRunRootCommands()
    {
        boolean retval = false;
        Process suProcess;

        try
        {
            suProcess = Runtime.getRuntime().exec("su");

            DataOutputStream os = new DataOutputStream(suProcess.getOutputStream());
            DataInputStream osRes = new DataInputStream(suProcess.getInputStream());

            if (null != os && null != osRes)
            {
                // Getting the id of the current user to check if this is root
                os.writeBytes("id\n");
                os.flush();

                String currUid = osRes.readLine();
                boolean exitSu = false;
                if (null == currUid)
                {
                    retval = false;
                    exitSu = false;
                    Log.d("ROOT", "Can't get root access or denied by user");
                }
                else if (true == currUid.contains("uid=0"))
                {
                    retval = true;
                    exitSu = true;
                    Log.d("ROOT", "Root access granted");
                }
                else
                {
                    retval = false;
                    exitSu = true;
                    Log.d("ROOT", "Root access rejected: " + currUid);
                }

                if (exitSu)
                {
                    os.writeBytes("exit\n");
                    os.flush();
                }
            }
        }
        catch (Exception e)
        {
            // Can't get root !
            // Probably broken pipe exception on trying to write to output stream (os) after su failed, meaning that the device is not rooted

            retval = false;
            Log.d("ROOT", "Root access rejected [" + e.getClass().getName() + "] : " + e.getMessage());
        }

        return retval;
    }

    public final String execute(String command)
    {
        boolean retval = false;
        String error = "";
        DataInputStream osRes;
        Process suProcess;
        StringBuilder output = null;

        try
        {
            if (null != command && command.length() > 0)
            {
                suProcess = Runtime.getRuntime().exec("su");

                DataOutputStream os = new DataOutputStream(suProcess.getOutputStream());
                osRes = new DataInputStream(suProcess.getInputStream());

                // Execute commands that require root access
                os.writeBytes(command + "\n");
                os.flush();

                os.writeBytes("exit\n");
                os.flush();

                try
                {
                    int suProcessRetval = suProcess.waitFor();
                    if (255 != suProcessRetval)
                    {
                        // Root access granted
                        retval = true;
                        BufferedReader r = new BufferedReader(new InputStreamReader(osRes));
                        output = new StringBuilder();
                        String line = "";
                        while ((line = r.readLine()) != null) {
                            output.append(line).append('\n');
                        }
                    }
                    else
                    {
                        // Root access denied
                        retval = false;
                    }
                }
                catch (Exception ex)
                {
                    error = "Error executing root action";
                    Log.e("ROOT", error, ex);
                }
            }
        }
        catch (IOException ex)
        {
            error = "Error: Can't get root access";
            Log.w("ROOT", error, ex);
        }
        catch (SecurityException ex)
        {
            error = "Error: Can't get root access";
            Log.w("ROOT", error, ex);
        }
        catch (Exception ex)
        {
            error = "Error executing internal operation";
            Log.w("ROOT", error, ex);
        }

        if (!retval) {
            return error;
        }

        return output.toString();
    }
}
