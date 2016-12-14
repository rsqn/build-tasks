package tech.rsqn.build.ant;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

public class Utils {

    public static String getLocalHostName() throws SocketException {
        String hostName = null;
        Enumeration<NetworkInterface> eIf = NetworkInterface.getNetworkInterfaces();
        while (hostName == null && eIf.hasMoreElements()) {
            NetworkInterface nIf = eIf.nextElement();
            Enumeration<InetAddress> eAddress = nIf.getInetAddresses();
            while (eAddress.hasMoreElements()) {
                InetAddress address = eAddress.nextElement();
                if (!address.isLoopbackAddress()) {
                    hostName = address.getHostName();
                }
            }
        }

        if ( hostName != null) {
            return hostName;
        }

        return "unableToResolve";
    }

}
