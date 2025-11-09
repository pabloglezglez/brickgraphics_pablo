package io;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.*;
import javax.swing.*;

/**
 * @author LD
 */
public class Log {
	private static Log instance;	
	private List<JTextField> fields;
	private PrintWriter out;
	private static final SimpleDateFormat TS_FMT = new SimpleDateFormat("HH:mm:ss.SSS");
    
	public enum Level { DEBUG, INFO, WARN, ERROR }
	private static volatile Level minLevel = Level.DEBUG; // default: show all
	
	public static void initializeLog(String fileName) throws IOException {
		instance = new Log();
		instance.fields = new LinkedList<JTextField>();
		FileOutputStream os = new FileOutputStream(new File(fileName), true);
		instance.out = new PrintWriter(os);
	}

	private Log() {
	}
	
	public static JTextField makeStatusBar() {
		JTextField tf = new JTextField();
		tf.setEditable(false);
		instance.fields.add(tf);
		return tf;
	}
	
	public static void log(String message) {
		// Infer level from conventional prefixes used across the codebase
		Level level = inferLevel(message);
		if (!shouldLog(level)) return;
		String ts = TS_FMT.format(new Date());
		String line = "[" + ts + "] " + message;
		System.out.println(line);
		for(JTextField tf : instance.fields) {
			tf.setText(message);
		}
		if(instance.out != null)
			instance.out.println(line);
	}
    
	public static void log(Level level, String message) {
		if (!shouldLog(level)) return;
		String ts = TS_FMT.format(new Date());
		String line = "[" + ts + "] " + message;
		System.out.println(line);
		for(JTextField tf : instance.fields) {
			tf.setText(message);
		}
		if(instance.out != null)
			instance.out.println(line);
	}

	public static void setMinimumLevel(Level level) {
		if (level != null) minLevel = level;
	}

	private static boolean shouldLog(Level level) {
		// Order is DEBUG < INFO < WARN < ERROR
		return level.ordinal() >= minLevel.ordinal();
	}

	private static Level inferLevel(String msg) {
		if (msg == null) return Level.INFO;
		String m = msg.trim();
		if (m.startsWith("DEBUG:")) return Level.DEBUG;
		if (m.startsWith("WARN:")) return Level.WARN;
		if (m.startsWith("ERROR:")) return Level.ERROR;
		return Level.INFO;
	}

	public static void log(Exception e) {
		log(Level.ERROR, e.getMessage());
		e.printStackTrace();
		if(instance.out != null)
			e.printStackTrace(instance.out);
	}
	
	public static void close() {
		if(instance.out == null)
			return;
		instance.out.flush();
		instance.out.close();
	}
}
