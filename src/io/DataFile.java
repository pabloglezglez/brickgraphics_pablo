package io;

import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.*;

import javax.imageio.ImageIO;

import java.util.Base64;

public class DataFile {
	private byte[] data;
	
	public DataFile(File file) throws IOException {
		data = Files.readAllBytes(Paths.get(file.getCanonicalPath()));
	}
	
	public DataFile(String base64) throws IOException {
		data = Base64.getDecoder().decode(base64.replaceAll("^\"|\"$", ""));
	}
	
	public DataFile(BufferedImage image) throws IOException {
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		ImageIO.write(image, "png", os);
		data = os.toByteArray();
	}
	
	public DataFile() {
		data = null;
	}
	
	public boolean isValid() {
		return data != null;
	}
	
	public String toBase64() {
		if(!isValid())
			return "";
		String out = Base64.getEncoder().encodeToString(data);
		return '"'+out+'"';
	}
	
	public InputStream fakeStream() {
		return new ByteArrayInputStream(data);
	}
}
