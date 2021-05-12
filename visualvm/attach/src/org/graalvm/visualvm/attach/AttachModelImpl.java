/*
 * Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
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

package org.graalvm.visualvm.attach;

import com.sun.tools.attach.VirtualMachine;
import org.graalvm.visualvm.application.Application;
import org.graalvm.visualvm.tools.attach.AttachModel;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import sun.tools.attach.HotSpotVirtualMachine;

/**
 *
 * @author Tomas Hurka
 */
class AttachModelImpl extends AttachModel {
    static final String LIVE_OBJECTS_OPTION = "-live";  // NOI18N
    static final String ALL_OBJECTS_OPTION = "-all";    // NOI18N
    static final String JCMD_VM_COMMAND_LINE = "VM.command_line";    // NOI18N
    static final String JCMD_JFR_DUMP = "JFR.dump";    // NOI18N
    static final String JCMD_JFR_DUMP_FILENAME = "filename";    // NOI18N
    static final String JCMD_JFR_DUMP_RECORDING = "recording";    // NOI18N
    static final String JCMD_JFR_CHECK = "JFR.check";   // NOI18N
    static final String JCMD_JFR_CHECK_RECORDING_ID = "recording=";     // NOI18N
    static final String JCMD_JFR_CHECK_RECORDING_ID1 = "Recording ";     // NOI18N
    static final String JCMD_JFR_START = "JFR.start";   // NOI18N
    static final String JCMD_JFR_UNLOCK_ID = "Use VM.unlock_commercial_features to enable"; // NOI18N
    static final String JCMD_UNLOCK_CF = "VM.unlock_commercial_features"; // NOI18N
    static final String JCMD_CF_ID = " unlocked.";   // NOI18N
    static final Logger LOGGER = Logger.getLogger(AttachModelImpl.class.getName());

    String pid;
    HotSpotVirtualMachine vm;
    Map<String,String> commandLineMap;
    Boolean oldJFR;
    Boolean jfrAvailable;
    
    AttachModelImpl(Application app) {
        pid = Integer.toString(app.getPid());
    }
    
    public synchronized Properties getSystemProperties() {
        try {
            return getVirtualMachine().getSystemProperties();
        } catch (IOException ex) {
            LOGGER.log(Level.INFO,"getSystemProperties",ex);    // NOI18N
        }
        return null;
    }
    
    public synchronized boolean takeHeapDump(String fileName) {
        try {
            InputStream in = getVirtualMachine().dumpHeap(fileName,LIVE_OBJECTS_OPTION);
            String out = readToEOF(in);
            if (out.length()>0) {
                LOGGER.log(Level.INFO,"takeHeapDump",out);  // NOI18N
            }
            return true;
        } catch (IOException ex) {
            LOGGER.log(Level.INFO,"takeHeapDump",ex);   // NOI18N
        }
        return false;
    }
    
    public synchronized String takeThreadDump() {
        try {
            InputStream in = getVirtualMachine().remoteDataDump("-l");  // NOI18N
            return readToEOF(in);
        } catch (IOException ex) {
            LOGGER.log(Level.INFO,"takeThreadDump",ex);     // NOI18N
        }
        return null;
    }
    
    public synchronized String printFlag(String name) {
        try {
            InputStream in = getVirtualMachine().printFlag(name);
            return readToEOF(in);
        } catch (IOException ex) {
            LOGGER.log(Level.INFO,"printFlag",ex);  // NOI18N
        }
        return null;
    }
    
    public synchronized void setFlag(String name, String value) {
        try {
            InputStream in = getVirtualMachine().setFlag(name,value);
            String out = readToEOF(in);
            if (out.length()>0) {
                LOGGER.log(Level.INFO,"setFlag",out);   // NOI18N
            }
        } catch (IOException ex) {
            LOGGER.log(Level.INFO,"setFlag",ex);    // NOI18N
        }
    }

    public synchronized HeapHistogramImpl takeHeapHistogram() {
        try {
            InputStream in = getVirtualMachine().heapHisto(ALL_OBJECTS_OPTION);
            HeapHistogramImpl h = new HeapHistogramImpl(in);
            in.close();
            return h;
        } catch (IOException ex) {
            LOGGER.log(Level.INFO,"takeHeapHistogram",ex);  // NOI18N
        }
        return null;
    }

    public String getCommandLine() {
        Map<String,String> cmdLineMap = getVMCommandLine();
        if (cmdLineMap != null) {
            return cmdLineMap.get("java_command");      // NOI18N
        }
        return null;
    }
    
    public String getJvmArgs() {
        Map<String,String> cmdLineMap = getVMCommandLine();
        if (cmdLineMap != null) {
            return cmdLineMap.get("jvm_args");          // NOI18N
        }
        return null;
    }

    public String getJvmFlags() {
        Map<String,String> cmdLineMap = getVMCommandLine();
        if (cmdLineMap != null) {
            return cmdLineMap.get("jvm_flags");         // NOI18N
        }
        return null;
    }

    public synchronized boolean isJfrAvailable() {
        if (jfrAvailable == null) {
            String recordings = executeJCmd(JCMD_JFR_CHECK);
            if (recordings == null) {
                jfrAvailable = Boolean.FALSE;
            } else {
                if (recordings.contains(JCMD_JFR_UNLOCK_ID)) {
                    jfrAvailable = unlockCommercialFeature();
                } else {
                    jfrAvailable = Boolean.TRUE;
                }
            }
        }
        return jfrAvailable;
    }

    public List<Long> jfrCheck() {
        if (!isJfrAvailable()) {
            throw new UnsupportedOperationException();
        }
        String recordings = executeJCmd(JCMD_JFR_CHECK);
        if (recordings == null) {
            return Collections.EMPTY_LIST;
        }
        String[] lines = recordings.split("\\r?\\n");       // NOI18N
        List<Long> recNumbers = new ArrayList(lines.length);

        for (String line : lines) {
            int index = line.indexOf(JCMD_JFR_CHECK_RECORDING_ID);
            if (index >= 0) {
                int recStart = index + JCMD_JFR_CHECK_RECORDING_ID.length();
                int recEnd = line.indexOf(' ', recStart);

                if (recEnd > recStart) {
                    String recordingNum = line.substring(recStart, recEnd);
                    recNumbers.add(Long.valueOf(recordingNum));
                    oldJFR = Boolean.TRUE;
                }
            } else if (line.startsWith(JCMD_JFR_CHECK_RECORDING_ID1)) {
                int recStart = JCMD_JFR_CHECK_RECORDING_ID1.length();
                int recEnd = line.indexOf(':', recStart);

                if (recEnd > recStart) {
                    String recordingNum = line.substring(recStart, recEnd);
                    recNumbers.add(Long.valueOf(recordingNum));
                    oldJFR = Boolean.FALSE;
                }
            }
        }
        return recNumbers;
    }

    public String takeJfrDump(long recording, String fileName) {
        if (!isJfrAvailable()) {
            throw new UnsupportedOperationException();
        }
        Map<String, Object> pars = new HashMap();
        pars.put(JCMD_JFR_DUMP_FILENAME, fileName);
        if (Boolean.TRUE.equals(oldJFR)) {
            pars.put(JCMD_JFR_DUMP_RECORDING, recording);
        }
        return executeJCmd(JCMD_JFR_DUMP, pars);
    }

    public boolean startJfrRecording() {
        if (!isJfrAvailable()) {
            throw new UnsupportedOperationException();
        }
        executeJCmd(JCMD_JFR_START);
        return true;
    }

    HotSpotVirtualMachine getVirtualMachine() throws IOException {
        if (vm == null) {
            try {
                vm = (HotSpotVirtualMachine) VirtualMachine.attach(pid);
            } catch (Exception x) {
                throw new IOException(x.getLocalizedMessage(),x);
            }
        }
        return vm;
    }
    
    private String executeJCmd(String command, Map<String,Object> pars) {
        StringBuilder commandLine = new StringBuilder(command);

        for (Map.Entry<String,Object> e : pars.entrySet()) {
            String par;
            String key = e.getKey();
            Object val = e.getValue();

            if (val == null) {
                par = key;
            } else {
                par = String.format("%s=%s", key, quoteString(val.toString())); // NOI18N
            }
            commandLine.append(' ').append(par);
        }
        return executeJCmd(commandLine.toString());
    }

    private static String quoteString(String val) {
        if (val.indexOf(' ')>=0) {
            return "\""+val+"\"";   //NOI18N
        }
        return val;
    }

    private boolean unlockCommercialFeature() {
        String ret = executeJCmd(JCMD_UNLOCK_CF);
        return ret.contains(JCMD_CF_ID);
    }

    private synchronized Map<String,String> getVMCommandLine() {
        if (commandLineMap == null) {
            String text = executeJCmd(JCMD_VM_COMMAND_LINE);
            commandLineMap = new HashMap();
            if (text != null) {
                String[] lines = text.split("\\R"); // NOI18N
                for (String line : lines) {
                    int offset = line.indexOf(':');     // NOI18N
                    if (offset != -1) {
                        String key = line.substring(0, offset).trim();
                        String value = line.substring(offset+1).trim();
                        commandLineMap.put(key, value);
                    }
                }
            }
        }
        return commandLineMap;
    }

    private synchronized String executeJCmd(String command) {
        try {
            InputStream in = getVirtualMachine().executeJCmd(command);
            return readToEOF(in);
        } catch (IOException ex) {
            LOGGER.log(Level.INFO,"executeJCmd",ex);    // NOI18N
        }
        return null;
    }

    private String readToEOF(InputStream in) throws IOException {
        StringBuffer buffer = new StringBuffer(1024);
        byte b[] = new byte[256];
        int n;
        
        do {
            n = in.read(b);
            if (n > 0) {
                String s = new String(b, 0, n, "UTF-8");    // NOI18N
                
                buffer.append(s);
            }
        } while (n > 0);
        in.close();
        return buffer.toString();
    }

    protected void finalize() throws Throwable {
        if (vm != null) vm.detach();
        super.finalize();
    }
    
}