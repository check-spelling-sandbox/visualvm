/*
 * Copyright (c) 1997, 2018, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package org.graalvm.visualvm.lib.jfluid.jps;

import org.graalvm.visualvm.lib.jfluid.utils.MiscUtils;
import sun.jvmstat.monitor.*;
import java.lang.management.ManagementFactory;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;


/**
 * This class is based on "jvmps" class from jvmps 2.0 written by Brian Doherty.
 * It provides functionality to identify all the JVMs currently running on the local machine.
 * Comments starting with //// are original comments from Brian.
 *
 * @author Tomas Hurka
 * @author Misha Dmitriev
 */
public class JpsProxy {

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    /** Returns the array of records for all running VMs capable of dynamic attach (JDK 1.6 and newer)*/
    public static RunningVM[] getRunningVMs() {
        String hostname = null;
        List vret = new ArrayList();

        try {
            HostIdentifier hostId = new HostIdentifier(hostname);
            MonitoredHost monitoredHost = MonitoredHost.getMonitoredHost(hostId);
            String selfName = ManagementFactory.getRuntimeMXBean().getName();

            //// get the list active VMs on the specified host.
            Set jvms = monitoredHost.activeVms();

            if (jvms.isEmpty()) {
                return null;
            }

            for (Iterator j = jvms.iterator(); j.hasNext();) {
                int lvmid = ((Integer) j.next()).intValue();

                if (selfName.startsWith(lvmid + "@")) { // myself

                    continue;
                }

                VmIdentifier id = null;
                MonitoredVm vm = null;
                String uriString = "//" + lvmid + "?mode=r"; // NOI18N

                try {
                    id = new VmIdentifier(uriString);
                    vm = monitoredHost.getMonitoredVm(id, 0);
                } catch (URISyntaxException e) {
                    //// this error should not occur as we are creating our own VMIdentifiers above based on a validated HostIdentifier.
                    //// This would be an unexpected condition.
                    MiscUtils.printWarningMessage("in jvmps, detected malformed VM Identifier: " + uriString + "; ignored"); // NOI18N

                    continue;
                } catch (MonitorException e) {
                    System.out.println("Ex " + e.getMessage());
                    e.printStackTrace();

                    //// it's possible that from the time we acquired the list of available jvms that a jvm has terminated. Therefore, it is
                    //// best just to ignore this error.
                    continue;
                } catch (Exception e) {
                    //// certain types of errors, such as access exceptions, can be encountered when attaching to a jvm.
                    //// These are reported as exceptions, not as some subclass of security exception.

                    //// FIXME - we should probably have some provision for logging these types of errors, or possibly just print out the
                    //// the Java Virtual Machine lvmid in a finally clause: System.out.println(String.valueOf(lvmid));
                    MiscUtils.printWarningMessage("in jvmps, for VM = " + String.valueOf(lvmid) + " got exception: " + e); // NOI18N

                    continue;
                }

                if (!isAttachable(vm)) {
                    monitoredHost.detach(vm);

                    continue;
                }

                String cmdString = MonitoredVmUtil.commandLine(vm);
                String mainClass = MonitoredVmUtil.mainClass(vm, true);
                String mainArgs = MonitoredVmUtil.mainArgs(vm);
                String vmArgs = MonitoredVmUtil.jvmArgs(vm);
                String vmFlags = MonitoredVmUtil.jvmFlags(vm);

                monitoredHost.detach(vm);

                RunningVM rvm = new RunningVM(lvmid, vmFlags, vmArgs, mainClass, mainArgs);
                vret.add(rvm);
            }
        } catch (MonitorException e) {
            String report = "in jvmps, got MonitorException"; // NOI18N

            if (e.getMessage() != null) {
                report += (" with message + " + e.getMessage()); // NOI18N
            }

            MiscUtils.printWarningMessage(report);

            return null;
        } catch (URISyntaxException e) {
            MiscUtils.printWarningMessage("in jvmps, got malformed Host Identifier: " + hostname); // NOI18N

            return null;
        }

        return (RunningVM[]) vret.toArray(new RunningVM[0]);
    }

    private static boolean isAttachable(MonitoredVm vm) {
        try {
            return MonitoredVmUtil.isAttachable(vm);
        } catch (MonitorException ex) {
            ex.printStackTrace();
            return false;
        }
    }
}
