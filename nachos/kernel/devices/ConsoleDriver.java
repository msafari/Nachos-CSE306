// ConsoleDriver.java
//
// Copyright (c) 2003 State University of New York at Stony Brook.
// All rights reserved.  See the COPYRIGHT file for copyright notice and 
// limitation of liability and disclaimer of warranty provisions.

package nachos.kernel.devices;

import java.io.UnsupportedEncodingException;

import nachos.Debug;
import nachos.machine.Console;
import nachos.machine.InterruptHandler;
import nachos.machine.NachosThread;
import nachos.kernel.Nachos;
import nachos.kernel.threads.Lock;
import nachos.kernel.threads.Semaphore;
import nachos.kernel.userprog.UserThread;

/**
 * This class provides for the initialization of the NACHOS console, and gives
 * NACHOS user programs a capability of outputting to the console. This driver
 * does not perform any input or output buffering, so a thread performing output
 * must block waiting for each individual character to be printed, and there are
 * no input-editing (backspace, delete, and the like) performed on input typed
 * at the keyboard.
 * 
 * Students will rewrite this into a full-fledged interrupt-driven driver that
 * provides efficient, thread-safe operation, along with echoing and
 * input-editing features.
 * 
 * @author Eugene W. Stark
 */
public class ConsoleDriver {

    /** Raw console device. */
    private Console console;

    public char[] inputBuf = new char[10];

    /** Echo buffer, size of 80 */
    public char[] echoBuf = new char[80];

    public int echoCounter = 0;

    private Semaphore echoBufferSem = new Semaphore("Echo Buff Sem", 0);

    public int outputCounter = 0;

    public int index = 0;

    public char[] outputBuf = new char[10];

    /** Lock used to ensure at most one thread trying to input at a time. */
    private Lock inputLock;

    /** Lock used to ensure at most one thread trying to output at a time. */
    private Lock outputLock;

    /** Semaphore used to indicate that an input character is available. */
    private Semaphore charAvail = new Semaphore("Console char avail", 0);

    /**
     * Semaphore used to indicate that output is ready to accept a new
     * character.
     */
    private Semaphore outputDone = new Semaphore("Console output done", 1);

    private Semaphore outBufferFull = new Semaphore("Output buffer full", 0);

    /** Interrupt handler used for console keyboard interrupts. */
    private InterruptHandler inputHandler;

    /** Interrupt handler used for console output interrupts. */
    private InterruptHandler outputHandler;

    /**
     * Initialize the driver and the underlying physical device.
     * 
     * @param console
     *            The console device to be managed.
     */
    public ConsoleDriver(Console console) {
	inputLock = new Lock("console driver input lock");
	outputLock = new Lock("console driver output lock");
	this.console = console;
	// Delay setting the interrupt handlers until first use.
    }

    /**
     * Create and set the keyboard interrupt handler, if one has not already
     * been set.
     */
    private void ensureInputHandler() {
	if (inputHandler == null) {
	    inputHandler = new InputHandler();
	    console.setInputHandler(inputHandler);
	}
    }

    /**
     * Create and set the output interrupt handler, if one has not already been
     * set.
     */
    private void ensureOutputHandler() {
	if (outputHandler == null) {
	    outputHandler = new OutputHandler();
	    console.setOutputHandler(outputHandler);
	}
    }

    /**
     * Wait for a character to be available from the console and then return the
     * character.
     */
    public char getChar() {
	ensureInputHandler();
	charAvail.P();
	Debug.ASSERT(console.isInputAvail());
	// Get the character
	char ch = console.getChar();

	// Add it to echo buffer
	if (ch == '\r' || ch == '\n') {

	    echoBuf[echoCounter] = '\r';
	    echoCounter++;

	    echoBuf[echoCounter] = '\n';
	    echoCounter++;

	} else if(ch == '\b'){
	    echoBuf[echoCounter] = '\b';
	    echoCounter++;

	    echoBuf[echoCounter] = '\0';
	    echoCounter++;
	    
	    echoBuf[echoCounter] = '\b';
	    echoCounter++;
	    putChar(echoBuf[echoCounter - 1]);
	    putChar(echoBuf[echoCounter - 1]);
	    putChar(echoBuf[echoCounter - 1]);
	}
	else {

	    echoBuf[echoCounter] = ch;
	    echoCounter++;
	}

	putChar(echoBuf[echoCounter - 1]);

	return ch;
    }

    /**
     * Print a single character on the console. If the console is already busy
     * outputting a character, then wait for it to finish before attempting to
     * output the new character. A lock is employed to ensure that at most one
     * thread at a time will attempt to print.
     * 
     * @param ch
     *            The character to be printed.
     */
    public void putChar(char ch) {

	ensureOutputHandler();
	UserThread curThrd = (UserThread) NachosThread.currentThread();

	if (!console.isOutputBusy()) {
	    console.putChar(ch);
	    curThrd.writeSize--;
	    curThrd.readSize--;

	}

	else {
	    if (outputCounter <= 9) {
		outputLock.acquire();
		outputBuf[outputCounter] = ch;
		outputCounter++;
		outputLock.release();
	    } else if (outputCounter == 10) {
		// block
		outputDone.P();

		for (int i = 0; i < outputCounter; i++) {
		    outBufferFull.P();
		    console.putChar(outputBuf[i]);
		    outputBuf[i] = '\0';
		}
		outputCounter = 0;
		curThrd.writeSize -= 10;
		curThrd.readSize -= 10;

		// put put charcter in here
		outputBuf[outputCounter++] = ch;
	    }

	    if (((UserThread) NachosThread.currentThread()).writeSize == outputCounter) {
		for (int i = 0; i < outputCounter; i++) {
		    outBufferFull.P();
		    console.putChar(outputBuf[i]);
		    outputBuf[i] = '\0';
		}
		outputCounter = 0;
		((UserThread) NachosThread.currentThread()).writeSize = 0;
		((UserThread) NachosThread.currentThread()).readSize = 0;

	    }
	}

	// A call to putChar() made when the device is idle (and hence when the
	// buffer is empty)
	// will require that the driver initiate output by calling
	// Console.putChar().

	// On the other hand, when the buffer is nonempty, the interrupt caused
	// by the completion
	// of printing of one character should trigger the start of printing of
	// the next character.

    }

    /**
     * Stop the console device. This removes the interrupt handlers, which
     * otherwise prevent the Nachos simulation from terminating automatically.
     */
    public void stop() {
	inputLock.acquire();
	console.setInputHandler(null);
	inputLock.release();
	outputLock.acquire();
	console.setOutputHandler(null);
	outputLock.release();
    }

    public char[] translate(byte[] b) throws UnsupportedEncodingException {
	String s = new String(b, "UTF-8");
	char[] c = s.toCharArray();
	return c;
    }

    /**
     * Interrupt handler for the input (keyboard) half of the console.
     */
    private class InputHandler implements InterruptHandler {

	@Override
	public void handleInterrupt() {
	    Debug.println('S', "Input handler called");
	    charAvail.V();
	    echoBufferSem.V();
	}

    }

    /**
     * Interrupt handler for the output (screen) half of the console.
     */
    private class OutputHandler implements InterruptHandler {

	Semaphore block = new Semaphore("block", 1);

	@Override
	public void handleInterrupt() {
	    Debug.println('S', "Output  handler called");

	    outBufferFull.V();
	    outputDone.V();
	    // echoBufferSem.V();

	}

    }
}
