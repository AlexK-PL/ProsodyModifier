package edu.upf.taln.voice.modulator;

import static org.junit.Assert.*;

import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.w3c.dom.Document;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

//import edu.upf.taln.speech.lipsync.commons.GenerationInfo;
//import edu.upf.taln.speech.lipsync.commons.SentenceData;

public class ProsodyModulatorTest {
	
	@Test
	public void generatePLAIN2XMLTest() throws Exception {
		
		//String InputText = "Hello boy!";
		String InputTextPath = "src/test/resources/sample_text.txt";
		String outputXMLPath = "src/test/resources/generatedXMLFile.xml";
		//Document expected = "src/test/resources/expected_maryXML.xml";
		
		BufferedReader br = new BufferedReader(new FileReader(InputTextPath));			
		String InputText = br.readLine();
		br.close();
				
		ProsodyModulator modulator2xml = new ProsodyModulator();
		
        Document docAcoustic = modulator2xml.convert2AcousticDoc(InputText);
        modulator2xml.saveDoc2XML(docAcoustic, outputXMLPath);
	}

	@Test
	public void generateXML2WAVTest() throws Exception {
		
		String inputXMLPath = "src/test/resources/generatedXMLFile.xml";
		String outputWAVPath = "src/test/resources/generatedWAVsound.wav";
		//String mappingMaryXMLPath = "src/test/resources/testSample_maryxml.txt";
		
		ProsodyModulator modulator2wav = new ProsodyModulator();
		
		modulator2wav.generateXML2WAV(inputXMLPath, outputWAVPath);				
	}
	
	@Test
	public void changePhonemeDurations() throws Exception {
		
		// The string vector with durations must coincide with the number of phonemes of the sentence
		String InputDurations[] = {"100", "100", "100", "500", "100", "700"};
		//Same number of TOKENS
		String BoundaryPauses [] = {"500", "200", "400"};
		
		String InputTextPath = "src/test/resources/sample_text.txt";
		String outputWAVPath = "src/test/resources/modifiedPhonemeDurations.wav";
		String outputXMLPath = "src/test/resources/modifiedPhonemeDurations.xml";
		
		BufferedReader br = new BufferedReader(new FileReader(InputTextPath));			
		String InputText = br.readLine();
		br.close();
		
		ProsodyModulator modulatorChangeDurations = new ProsodyModulator();
		
		Document docAcoustic = modulatorChangeDurations.convert2AcousticDoc(InputText);
		Document docAcousticModified = modulatorChangeDurations.modifyPhonemeDurations(docAcoustic, InputDurations);
		Document docDurationsBoundaries = modulatorChangeDurations.insertWordPauseBoundaries(docAcousticModified, BoundaryPauses);
		modulatorChangeDurations.generateDoc2WAV(docDurationsBoundaries, outputWAVPath);
		modulatorChangeDurations.saveDoc2XML(docDurationsBoundaries, outputXMLPath);
		
	}
	
	@Test
	public void changeF0Contour() throws Exception {
		
		// A matrix with all f0 contour information in semitones. Rows are the number of input phonemes, and columns
		// the contour information equally distributed
		Double[][] multiInfoSemitones = new Double[][] {
			{0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 1.0},
			{1.0, 1.0, 2.0, 2.0, 2.0, 3.0, 3.0, 4.2, 4.2, 4.2},
			{4.0, 4.0, 3.5, 2.5, 2.0, 2.0, 2.0, 2.0, 2.0, 1.9},
			{2.5, 2.5, 2.5, 2.8, 3.0, 3.0, 3.2, 3.3, 3.3, 3.4},
			{0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0},
			{-0.5, -0.35, -0.15, 0.0, 1.0, 1.0, 2.5, 3.5, 4.5, 4.5}
		};
		
		String InputDurations[] = {"80", "120", "100", "300", "50", "320"};
		//Same number of TOKENS (a punctuation next to a word is considered a unique element)
		String BoundaryPauses [] = {"250", "270"};
		
		String InputTextPath = "src/test/resources/sample_text.txt";
		String outputWAVPath = "src/test/resources/modifiedF0Contour.wav";
		String outputXMLPath = "src/test/resources/modifiedF0Contour.xml";
		// Only for 1 single line
		BufferedReader br = new BufferedReader(new FileReader(InputTextPath));			
		String InputText = br.readLine();
		br.close();
		
		ProsodyModulator modulatorChangeDurations = new ProsodyModulator();
		
		Document docAcoustic = modulatorChangeDurations.convert2AcousticDoc(InputText);
		Document docAcousticModifiedDurations = modulatorChangeDurations.modifyPhonemeDurations(docAcoustic, InputDurations);
		Document docAcousticModified = modulatorChangeDurations.modifyPitchContourHz(docAcousticModifiedDurations, multiInfoSemitones);
		Document docDurationsBoundaries = modulatorChangeDurations.insertWordPauseBoundaries(docAcousticModified, BoundaryPauses);
		modulatorChangeDurations.generateDoc2WAV(docDurationsBoundaries, outputWAVPath);
		modulatorChangeDurations.saveDoc2XML(docDurationsBoundaries, outputXMLPath);
		
	}
	
}
