// ConsoleDriver.java
//
// Copyright (c) 2003 State University of New York at Stony Brook.
// All rights reserved.  See the COPYRIGHT file for copyright notice and 
// limitation of liability and disclaimer of warranty provisions.

package nachos.kernel.devices;

import nachos.Debug;
import nachos.machine.Console;
import nachos.machine.InterruptHandler;
import nachos.kernel.threads.Lock;
import nachos.kernel.threads.Semaphore;

/**
 * This class provides for the initialization of the NACHOS console,
 * and gives NACHOS user programs a capability of outputting to the console.
 * This driver does not perform any input or output buffering, so a thread
 * performing output must block waiting for each individual character to be
 * printed, and there are no input-editing (backspace, delete, and the like)
 * performed on input typed at the keyboard.
 * 
 * Students will rewrite this into a full-fledged interrupt-driven driver
 * that provides efficient, thread-safe operation, along with echoing and
 * input-editing features.
 * 
 * @author Eugene W. Stark
 */
public class ConsoleDriver {
    
    /** Raw console device. */
    private Console console;

    /** Lock used to ensure at most one thread trying to input at a time. */
    private Lock inputLock;
    
    /** Lock used to ensure at most one thread trying to output at a time. */
    private Lock outputLock;
    
    /** Semaphore used to indicate that an input character is available. */
    private Semaphore charAvail = new Semaphore("Console char avail", 0);
    
    /** Semaphore used to indicate that output is ready to accept a new character. */
    private Semaphore outputDone = new Semaphore("Console output done", 1);
    
    /** Interrupt handler used for console keyboard interrupts. */
    private InterruptHandler inputHandler;
    
    /** Interrupt handler used for console output interrupts. */
    private InterruptHandler outputHandler;

    /**
     * Initialize the driver and the underlying physical device.
     * 
     * @param console  The console device to be managed.
     */
    public ConsoleDriver(Console console) {
	inputLock = new Lock("console driver input lock");
	outputLock = new Lock("console driver output lock");
	this.console = console;
	// Delay setting the interrupt handlers until first use.
    }
    
    /**
     * Create and set the keyboard interrupt handler, if one has not
     * already been set.
     */
    private void ensureInputHandler() {
	if(inputHandler == null) {
	    inputHandler = new InputHandler();
	    console.setInputHandler(inputHandler);
	}
    }

    /**
     * Create and set the output interrupt handler, if one has not
     * already been set.
     */
    private void ensureOutputHandler() {
	if(outputHandler == null) {
	    outputHandler = new OutputHandler();
	    console.setOutputHandler(outputHandler);
	}
    }

    /**
     * Wait for a character to be available from the console and then
     * return the character.
     */
    public char getChar() {
	inputLock.acquire();
	ensureInputHandler();
	charAvail.P();
	Debug.ASSERT(console.isInputAvail());
	char ch = console.getChar();
	inputLock.release();
	return ch;
    }

    /**
     * Print a single character on the console.  If the console is already
     * busy outputting a character, then wait for it to finish before
     * attempting to output the new character.  A lock is employed to ensure
     * that at most one thread at a time will attempt to print.
     *
     * @param ch The character to be printed.
     */
    public void putChar(char ch) {
	outputLock.acquire();
	ensureOutputHandler();
	outputDone.P();
	Debug.ASSERT(!console.isOutputBusy());
	console.putChar(ch);
	outputLock.release();
    }

    /**
     * Stop the console device.
     * This removes the interrupt handlers, which otherwise prevent the
     * Nachos simulation from terminating automatically.
     */
    public void stop() {
	inputLock.acquire();
	console.setInputHandler(null);
	inputLock.release();
	outputLock.acquire();
	console.setOutputHandler(null);
	outputLock.release();
    }
    
    /**
     * Interrupt handler for the input (keyboard) half of the console.
     */
    private class InputHandler implements InterruptHandler {
	
	@Override
	public void handleInterrupt() {
	    charAvail.V();
	}
	
    }
    
    /**
     * Interrupt handler for the output (screen) half of the console.
     */
    private class OutputHandler implements InterruptHandler {
	
	@Override
	public void handleInterrupt() {
	    outputDone.V();
	}
 	
    }
}
