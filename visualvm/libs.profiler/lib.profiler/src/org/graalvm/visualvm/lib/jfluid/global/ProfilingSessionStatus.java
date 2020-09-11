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

package org.graalvm.visualvm.lib.jfluid.global;

import org.graalvm.visualvm.lib.jfluid.wireprotocol.InternalStatsResponse;


/**
 * Various data pertinent to the profiling session. Note that this class is used by both client and back end,
 * although some data at the back end side is contained in the reduced form, to reduce memory usage.
 *
 * @author Tomas Hurka
 * @author Misha Dmitriev
 * @author Adrian Mos
 */
public class ProfilingSessionStatus {
    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    public static final int N_TIMER_CONSTANTS = 5;

    //EJB Work: The index pointing to the class for code region instrumentation. It is the first class in the array of classes, as it should be the only class there.
    public static final int CODE_REGION_CLASS_IDX = 0;

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    // This internal statistics record is filled in at the client side only upon application termination. It exists just to
    // allow the user to obtain statistics after the target app/VM has already completed execution.
    public InternalStatsResponse savedInternalStats;

    // Full (including minor version) target JDK version string - for example 1.5.0_12
    public String fullTargetJDKVersionString;

    // Data for both code region and method group instrumentation. In the latter case this data is for the root method.
    public String instrClassLoaderName;

    // Command line arguments of the JVM, and the full Java command, that includes main class (or .jar) plus its arguments
    public String javaCommand;

    // Command line arguments of the JVM, and the full Java command, that includes main class (or .jar) plus its arguments
    public String jvmArguments;

    // Target JDK version string, as returned by Platform.getJDKVersionString()
    public String targetJDKVersionString;

    // Target machine OS, as returned by System.getProperty("os.name")
    public String targetMachineOSName;

    // true if target JDK class file verifier accepts our constructor instrumentation
    public boolean canInstrumentConstructor;

    // Important timer-related characteristics of instrumentation that we inject into the TA.
    // Each array has 5 elements:
    // o value when absolute timer is used;
    // o value when thread-local CPU timer is used;
    // o value of inner/outer absolute time when both timers are used;
    // o value of inner/outer thread-local CPU time when both timers are used.
    // o value when sampled instrumentation is used.
    // ALL these values (including thread-local CPU ones, see comments in ProfilerCalibrator for explanation) are measured in
    // absolute timer's counts (which is anything on Windows and nanoseconds on Solaris)
    public double[] methodEntryExitCallTime = new double[N_TIMER_CONSTANTS]; // Elements #2 and #3 are actually the same
    public double[] methodEntryExitInnerTime = new double[N_TIMER_CONSTANTS];
    public double[] methodEntryExitOuterTime = new double[N_TIMER_CONSTANTS];
    public long[] timerCountsInSecond = new long[2];
    public boolean absoluteTimerOn;
    public boolean remoteProfiling = false;
    public boolean runningInAttachedMode; // true if attached to target JVM, false if started it from client
    public boolean startProfilingPointsActive; // Indicates that inCallGraph should be set by profiling point handler code and NOT rootMethodEntry
    public volatile boolean targetAppRunning;
    public boolean threadCPUTimerOn;
    public int currentInstrType;
    public int instrEndLine;
    public int instrScheme;
    public int instrStartLine;

    // Server's timestamp of the moment when CPU results dump is requested. Used to display absolute call graph time correctly even
    // when no absolute timestamps for methods are collected. Note that we could probably do the same thing on a per-thread basis
    // and thus have thread CPU call graph time always available - but that seems to be too much work for now.
    public long dumpAbsTimeStamp;

    // Target machine maximum heap size returned by Runtime.maxMemory()
    public long maxHeapSize;

    // Target machine statup time returned by Timers.getCurrentTimeInCounts()
    public long startupTimeInCounts;

    // Target machine startup time returned by System.currentTimeMillis()
    public long startupTimeMillis;
    private TransactionalSupport transaction = new TransactionalSupport();
    private int[] allocatedInstancesCount;
    private int[] classLoaderIds;
    private String[] classNames;
    private String[] instrMethodClasses;
    private boolean[] instrMethodInvoked;
    private String[] instrMethodNames;
    private String[] instrMethodSignatures;

    // Data for the case of object allocations instrumentation
    private int nInstrClasses;

    // Data for the case of method group instrumentation
    private int nInstrMethods;

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public int[] getAllocatedInstancesCount() {
        beginTrans(false);

        try {
            return allocatedInstancesCount;
        } finally {
            endTrans();
        }
    }

    public int[] getClassLoaderIds() {
        beginTrans(false);

        try {
            return classLoaderIds;
        } finally {
            endTrans();
        }
    }

    public String[] getClassNames() {
        beginTrans(false);

        try {
            return classNames;
        } finally {
            endTrans();
        }
    }

    public String[] getInstrMethodClasses() {
        beginTrans(false);

        try {
            return instrMethodClasses;
        } finally {
            endTrans();
        }
    }

    public boolean[] getInstrMethodInvoked() {
        beginTrans(false);

        try {
            return instrMethodInvoked;
        } finally {
            endTrans();
        }
    }

    public void setInstrMethodNames(String[] value) {
        beginTrans(true);

        try {
            instrMethodNames = value;
        } finally {
            endTrans();
        }
    }

    public String[] getInstrMethodNames() {
        beginTrans(false);

        try {
            return instrMethodNames;
        } finally {
            endTrans();
        }
    }

    public void setInstrMethodSignatures(String[] value) {
        beginTrans(true);

        try {
            instrMethodSignatures = value;
        } finally {
            endTrans();
        }
    }

    public String[] getInstrMethodSignatures() {
        beginTrans(false);

        try {
            return instrMethodSignatures;
        } finally {
            endTrans();
        }
    }

    public int getNInstrClasses() {
        beginTrans(false);

        try {
            return nInstrClasses;
        } finally {
            endTrans();
        }
    }

    public int getNInstrMethods() {
        beginTrans(false);

        try {
            return nInstrMethods;
        } finally {
            endTrans();
        }
    }

    public int getStartingMethodId() {
        beginTrans(false);

        try {
            if (nInstrMethods > 0) {
                return nInstrMethods;
            } else {
                return 1;
            }
        } finally {
            endTrans();
        }
    }

    public void setTimerTypes(boolean absolute, boolean threadCPU) {
        absoluteTimerOn = absolute;
        threadCPUTimerOn = threadCPU;
    }

    public void beginTrans(boolean mutable) {
        transaction.beginTrans(mutable);
    }

    public boolean collectingTwoTimeStamps() {
        return (absoluteTimerOn && threadCPUTimerOn);
    }

    public void endTrans() {
        transaction.endTrans();
    }

    // ----------------------------------- Recursive method instrumentation management ------------------------------------------
    public void resetInstrClassAndMethodInfo() {
        beginTrans(true);

        try {
            nInstrMethods = 0;
            instrMethodClasses = instrMethodNames = instrMethodSignatures = null;
            instrMethodInvoked = null;
            nInstrClasses = 0;
            allocatedInstancesCount = null;
            classNames = new String[0];
        } finally {
            endTrans();
        }
    }

    // --------------------- Object allocations/liveness accounting management ------------------------------

    /**
     * Takes the delta in the number of profiled classes and their names, and updates internal data structures.
     * Should be used only at client side.
     */
    public void updateAllocatedInstancesCountInfoInClient(String addedClassName) {
        beginTrans(true);

        try {
            if ((nInstrClasses == 0) || (nInstrClasses == classNames.length)) {
                boolean firstTime = (nInstrClasses == 0);
                int newSize = firstTime ? 50 : ((nInstrClasses * 3) / 2);
                int[] newAllocInstCount = new int[newSize];
                String[] newClassNames = new String[newSize];

                if (!firstTime) {
                    System.arraycopy(allocatedInstancesCount, 0, newAllocInstCount, 0, nInstrClasses);
                    System.arraycopy(classNames, 0, newClassNames, 0, nInstrClasses);
                }

                allocatedInstancesCount = newAllocInstCount;
                classNames = newClassNames;
            }

            classNames[nInstrClasses++] = addedClassName;
        } finally {
            endTrans();
        }
    }

    /** Same as above, but takes the total number of classes, and should be used only at server side. */
    public void updateAllocatedInstancesCountInfoInServer(int nTotalClasses) {
        beginTrans(true);

        try {
            boolean firstTime = (nInstrClasses == 0);
            int oldSize = firstTime ? 0 : allocatedInstancesCount.length;
            int newLen = nTotalClasses;

            if (oldSize < newLen) {
                int newSize = newLen * 2;
                int[] newAllocInstCount = new int[newSize];

                if (!firstTime) {
                    System.arraycopy(allocatedInstancesCount, 0, newAllocInstCount, 0, nInstrClasses);
                }

                allocatedInstancesCount = newAllocInstCount;
            }

            nInstrClasses = nTotalClasses;
        } finally {
            endTrans();
        }
    }

    /**
     * This method updates information about instrumented methods (class, name, signature, class loader for class etc.)
     * for a group of methods. Can be used at both client and server side.
     */
    public void updateInstrMethodsInfo(int nClasses, int nMethods, String[] classes, int[] loaderIds, int[] nMethodsInClass,
                                       String[] methodNames, String[] methodSignatures, boolean[] isMethodLeaf) {
        if (nClasses == 0) {
            return;
        }

        beginTrans(true);

        try {
            boolean firstTime = (nInstrMethods == 0);
            int oldSize = firstTime ? 0 : ((classes != null) ? instrMethodNames.length : instrMethodInvoked.length);
            int emptyCell = (firstTime ? 1 : 0);
            int nAddedMethods = nMethods + emptyCell;
            int newLen = nInstrMethods + nMethods + emptyCell;

            if (oldSize < newLen) { // Grow arrays

                int newSize = newLen * 2;

                if (classes != null) { // Client side execution

                    String[] newClasses = new String[newSize];
                    int[] newLoaderIds = new int[newSize];
                    String[] newMethods = new String[newSize];
                    String[] newSignatures = new String[newSize];

                    if (!firstTime) {
                        System.arraycopy(instrMethodClasses, 0, newClasses, 0, nInstrMethods);
                        System.arraycopy(classLoaderIds, 0, newLoaderIds, 0, nInstrMethods);
                        System.arraycopy(instrMethodNames, 0, newMethods, 0, nInstrMethods);
                        System.arraycopy(instrMethodSignatures, 0, newSignatures, 0, nInstrMethods);
                    }

                    instrMethodClasses = newClasses;
                    classLoaderIds = newLoaderIds;
                    instrMethodNames = newMethods;
                    instrMethodSignatures = newSignatures;
                } else { // Server side execution

                    boolean[] newMethodInvoked = new boolean[newSize];

                    if (!firstTime) {
                        System.arraycopy(instrMethodInvoked, 0, newMethodInvoked, 0, nInstrMethods);
                    }

                    instrMethodInvoked = newMethodInvoked;
                }
            }

            if (classes != null) { // Client side execution

                if (firstTime) {
                    instrMethodClasses[0] = "Thread"; // NOI18N
                    classLoaderIds[0] = 0;
                    instrMethodNames[0] = ""; // NOI18N
                    instrMethodSignatures[0] = ""; // NOI18N
                }

                int idx = nInstrMethods + emptyCell;

                for (int i = 0; i < nClasses; i++) {
                    for (int j = 0; j < nMethodsInClass[i]; j++) {
                        instrMethodClasses[idx] = classes[i];
                        classLoaderIds[idx] = loaderIds[i];
                        idx++;
                    }
                }

                System.arraycopy(methodNames, 0, instrMethodNames, nInstrMethods + emptyCell, nMethods);
                System.arraycopy(methodSignatures, 0, instrMethodSignatures, nInstrMethods + emptyCell, nMethods);
            } else { // Server side execution

                if (isMethodLeaf != null) {
                    System.arraycopy(isMethodLeaf, 0, instrMethodInvoked, nInstrMethods + emptyCell, nMethods);
                }

                if (instrScheme == CommonConstants.INSTRSCHEME_EAGER) {
                    for (int i = nInstrMethods; i < (nInstrMethods + nAddedMethods); i++) {
                        instrMethodInvoked[i] = true;
                    }
                }
            }

            nInstrMethods += nAddedMethods;
        } finally {
            endTrans();
        }

        //System.err.println("*** Profiler Engine: nMethods = " + nMethods + ", nInstrMethods = " + nInstrMethods + ", instrMethodInvoked.len = " + instrMethodInvoked.length);
    }

    /**
     * This method adds information about a single instrumented method. Intended to be used on client side only.
     */
    public void updateInstrMethodsInfo(String className, int loaderId, String methodName, String methodSignature) {
        //System.err.println("*** Profiler Engine: updateInstr() called for " + className + "." + methodName + ", nis = " + nInstrMethods);
        beginTrans(true);

        try {
            boolean firstTime = (nInstrMethods == 0);
            int oldSize = firstTime ? 0 : instrMethodNames.length;
            int newLen = nInstrMethods + 1;
            int emptyCell = (firstTime ? 1 : 0);
            int nAddedMethods = 1 + emptyCell;

            if (oldSize < newLen) { // Grow arrays

                int newSize = newLen * 2;

                String[] newClasses = new String[newSize];
                int[] newLoaderIds = new int[newSize];
                String[] newMethods = new String[newSize];
                String[] newSignatures = new String[newSize];

                if (!firstTime) {
                    System.arraycopy(instrMethodClasses, 0, newClasses, 0, nInstrMethods);
                    System.arraycopy(classLoaderIds, 0, newLoaderIds, 0, nInstrMethods);
                    System.arraycopy(instrMethodNames, 0, newMethods, 0, nInstrMethods);
                    System.arraycopy(instrMethodSignatures, 0, newSignatures, 0, nInstrMethods);
                }

                instrMethodClasses = newClasses;
                classLoaderIds = newLoaderIds;
                instrMethodNames = newMethods;
                instrMethodSignatures = newSignatures;
            }

            if (firstTime) {
                instrMethodClasses[0] = "Thread"; // NOI18N
                classLoaderIds[0] = 0;
                instrMethodNames[0] = ""; // NOI18N
                instrMethodSignatures[0] = ""; // NOI18N
            }

            int idx = nInstrMethods + emptyCell;
            instrMethodClasses[idx] = className;
            classLoaderIds[idx] = loaderId;
            instrMethodNames[idx] = methodName;
            instrMethodSignatures[idx] = methodSignature;

            nInstrMethods += nAddedMethods;
        } finally {
            endTrans();
        }

        //System.err.println("*** Profiler Engine: nInstrMethods = " + nInstrMethods + ", instrMethodNames.len = " + instrMethodNames.length);
    }
}
