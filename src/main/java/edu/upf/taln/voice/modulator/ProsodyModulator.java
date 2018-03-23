package edu.upf.taln.voice.modulator;

import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

import org.w3c.dom.Node;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.traversal.DocumentTraversal;
import org.w3c.dom.traversal.NodeFilter;
import org.w3c.dom.traversal.NodeIterator;
import org.w3c.dom.traversal.TreeWalker;

import marytts.LocalMaryInterface;
import marytts.exceptions.MaryConfigurationException;
import marytts.exceptions.SynthesisException;
import marytts.util.data.audio.MaryAudioUtils;
import marytts.datatypes.MaryXML;
import marytts.util.MaryUtils;
import marytts.util.dom.DomUtils;
import marytts.util.dom.MaryDomUtils;
import marytts.util.dom.NameNodeFilter;
import marytts.util.math.MathUtils;
import marytts.util.math.Polynomial;
import marytts.util.string.StringUtils;

public class ProsodyModulator {

    private final LocalMaryInterface mary;

    public ProsodyModulator() throws MaryConfigurationException {
        mary = new LocalMaryInterface();
	}    
	
	public Document convert2AcousticDoc(String inputText) throws SynthesisException, TransformerException {
		
		mary.setInputType("TEXT");
		mary.setOutputType("ACOUSTPARAMS");
		
		Document OriginalMaryXML = mary.generateXML(inputText);
		return OriginalMaryXML;		
	}
	
	public void saveDoc2XML(Document doc, String outputXMLPath) throws SynthesisException, TransformerException {

		//Save Document type into a xml file
		Transformer transformer = TransformerFactory.newInstance().newTransformer();
		Result output = new StreamResult(new File(outputXMLPath));
		Source input = new DOMSource(doc);
		transformer.transform(input, output);
	}
	
	public void generateXML2WAV(String inputXMLPath, String outputWAVPath) throws Exception {
		
		//throw new Exception("AAAAAAAAAAAAAH");
		
		// Get the xml file of the input argument 'text'
		File fXmlFile = new File(inputXMLPath);
		DocumentBuilder dBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
		Document docXML = dBuilder.parse(fXmlFile);
		// Generate audio
		mary.setInputType("ACOUSTPARAMS");
		mary.setOutputType("AUDIO");
		AudioInputStream generatedAudio = mary.generateAudio(docXML);
		double[] samples = MaryAudioUtils.getSamplesAsDoubleArray(generatedAudio);
        MaryAudioUtils.writeWavFile(samples, outputWAVPath, generatedAudio.getFormat());	
	}
	
	public void generateDoc2WAV(Document doc, String outputWAVPath) throws Exception {
		
		// Generate audio
		mary.setInputType("ACOUSTPARAMS");
		mary.setOutputType("AUDIO");
		AudioInputStream generatedAudio = mary.generateAudio(doc);
		double[] samples = MaryAudioUtils.getSamplesAsDoubleArray(generatedAudio);
        MaryAudioUtils.writeWavFile(samples, outputWAVPath, generatedAudio.getFormat());	
	}
	
	public Document modifyPhonemeDurations(Document doc, String InputDurations[]) throws Exception {
		// Modify from ACOUSTICPARAMS MaryXML file the durations and end time of each phoneme
		NodeList sentences = doc.getElementsByTagName(MaryXML.SENTENCE);
		//System.out.println(sentences.getLength());
		for (int k = 0; k < sentences.getLength(); k++) {
			Element sentence = (Element) sentences.item(k);
			NodeList nl = sentence.getElementsByTagName("ph");
			
			int L = InputDurations.length;
			//System.out.println(String.valueOf(nl.getLength()));
			assert nl.getLength() == L;
			
			int phcounter = 0;
			double phdurations = 0;
			for (int i = 0; i < nl.getLength(); i++) {
				//These elements should be 'ph'
				Element e = (Element) nl.item(i);
				assert "ph".equals(e.getNodeName()) : "NodeList should contain 'ph' elements only";
				if (!e.hasAttribute("d") && !e.hasAttribute("end")) {
					continue;
				}else {
					e.setAttribute("d", InputDurations[phcounter] + "");
					if(i == 0) {
						phdurations = Double.parseDouble(InputDurations[phcounter])/1000;

					} else {
						phdurations = phdurations + (Double.parseDouble(InputDurations[phcounter])/1000);
					}
					e.setAttribute("end", Double.toString(phdurations));				
					phcounter++;
				}
			}
			//System.out.println(String.valueOf(phcounter));
		}
		return doc;
	}
	
	public Document insertWordPauseBoundaries(Document doc, String BoundaryPauses[]) throws Exception {
		// Add a boundary element after each token ending to control pauses between words
		int bi = 1;
		//For now, there is only one sentence for testing
		NodeList sentences = doc.getElementsByTagName(MaryXML.SENTENCE);
		for (int k = 0; k < sentences.getLength(); k++) {
			Element sentence = (Element) sentences.item(k);
			NodeList tkns = sentence.getElementsByTagName(MaryXML.TOKEN);
			for (int j = 0; j < tkns.getLength()-1; j++) {
				Element token = (Element) tkns.item(j);
				Element nextToken = null;
				if(j<tkns.getLength()) {
					nextToken = (Element) tkns.item(j+1);
				} 
				Element boundary = null;
				TreeWalker tw = ((DocumentTraversal) doc).createTreeWalker(DomUtils.getAncestor(token, MaryXML.SENTENCE),
						NodeFilter.SHOW_ELEMENT, new NameNodeFilter(new String[] { MaryXML.BOUNDARY, MaryXML.TOKEN }), false);
				tw.setCurrentNode(token);
				Element next = (Element) tw.nextNode();
				if (next != null && next.getTagName().equals(MaryXML.BOUNDARY)) {
					boundary = next;
				} /*else if (isPunctuation(nextToken)) {
					// if the current token is punctuation, we also look for a
					// boundary before the current token
					tw.setCurrentNode(token);
					Element prev = (Element) tw.previousNode();
					if (prev != null && prev.getTagName().equals(MaryXML.BOUNDARY)) {
						boundary = prev;
					}
				}*/
				// Reuse a boundary tag if it has the same parent as the token
				if (boundary != null && boundary.getParentNode().equals(token.getParentNode())) {
					// the break index:
					if (bi > 0) {
						boundary.setAttribute("breakindex", String.valueOf(bi));
						boundary.setAttribute("duration", BoundaryPauses[j]);
							//System.out.println(String.valueOf(BoundaryPauses[j]));
						//boundary.setAttribute("duration", String.valueOf(50));
					}
				} else { // no boundary tag yet, introduce one
					// First verify that we have a valid parent element
					if (token.getParentNode() == null) {
						continue;
					}
					// Make sure not to insert the new boundary
					// in the middle of an <mtu> element:
					Element eIn = (Element) token.getParentNode();
					Element eBefore = MaryDomUtils.getNextSiblingElement(token);
					// Now change these insertion references in case token
					// is the last one in an <mtu> tag.
					Element mtu = (Element) MaryDomUtils.getHighestLevelAncestor(token, MaryXML.MTU);
					if (mtu != null) {
						if (MaryDomUtils.isLastOfItsKindIn(token, mtu)) {
							eIn = (Element) mtu.getParentNode();
							eBefore = MaryDomUtils.getNextSiblingElement(mtu);
						} else {
							// token is in the middle of an mtu - don't insert boundary
							continue;
						}
					}
					if(nextToken != null && isPunctuation(nextToken)) {
						continue;
					}else {
					// Now the boundary tag is to be inserted.
					boundary = MaryXML.createElement(doc, MaryXML.BOUNDARY);
					boundary.setAttribute("breakindex", String.valueOf("1"));
					boundary.setAttribute("duration", BoundaryPauses[j]);
					//System.out.println(String.valueOf(BoundaryPauses[j]));
					//boundary.setAttribute("duration", String.valueOf(100));
					eIn.insertBefore(boundary, eBefore);
					}
				}
			}
		}
		return doc;
	}
	
	public Document modifyPitchContourHz(Document doc, Double semitones[][]) {
		
		NodeList sentences = doc.getElementsByTagName(MaryXML.SENTENCE);
		//System.out.println(sentences.getLength());
		for (int k = 0; k < sentences.getLength(); k++) {
			Element sentence = (Element) sentences.item(k);
			
			//Check whether a prosody element is added. If so, we need to remove the values.
			NodeList phrases = sentence.getElementsByTagName(MaryXML.PHRASE);
			if(phrases.getLength() > 1) {
				NodeList prosody = doc.getElementsByTagName(MaryXML.PROSODY);
				for (int p = 0; p < prosody.getLength(); p++) {
					Element pr = (Element) prosody.item(p);
					pr.setAttribute("pitch", "0%");
					pr.setAttribute("range", "0%");
				}
			}
			
			NodeList nl = sentence.getElementsByTagName("ph");
			//assert nl.getLength() == L;
			String formattedTargetValue;
			for (int i = 0; i < nl.getLength(); i++) {
				//These elements should be 'ph'
				Element e = (Element) nl.item(i);
				assert "ph".equals(e.getNodeName()) : "NodeList should contain 'ph' elements only";
				//int[] f0Targets = StringUtils.parseIntPairs(e.getAttribute("f0"));
				//double meanF0 = 0.0;
				//for (int j = 0, len = f0Targets.length / 2; j < len; j++) {
					//int f0Value = f0Targets[2 * j + 1];
					//meanF0 = meanF0 + f0Value;
				//}
				//Pitch average of the phoneme (we assume is confident as there is no much variability at phoneme level)
				//meanF0 = meanF0/(f0Targets.length/2);
				int meanPitchFemaleVoice = 176; //Hz 
				int lengthST = semitones[1].length;
				formattedTargetValue = "";
				double st = 0.0;
				for (int h = 0; h < lengthST; h++) {
					st = semitones[i][h];
					//Semitone to Hz convertion based on "Clinical Measurement of Speech and Voice" equation
					double f0out = meanPitchFemaleVoice*Math.pow(10,(st/39.86));
					//Uniform distribution of f0 contour values depending on the number of these values. They are located in 
					//the center of each group, instead of one side.
					formattedTargetValue += "(" + Integer.toString((int) h*(100/lengthST) + (int) Math.floor((100/lengthST)/2)) + ","
							+ Integer.toString((int) f0out) + ")";
				}
				if (formattedTargetValue.length() > 0)
					e.setAttribute("f0", formattedTargetValue);
			}
		}
		
		return doc;
	}
	
	protected boolean isPunctuation(Element token) {
		if (token == null)
			throw new NullPointerException("Received null token");
		if (!token.getTagName().equals(MaryXML.TOKEN))
			throw new IllegalArgumentException("Expected <" + MaryXML.TOKEN + "> element, got <" + token.getTagName() + ">");

		String tokenText = MaryDomUtils.tokenText(token);

		return tokenText.equals(",") || tokenText.equals(".") || tokenText.equals("?") || tokenText.equals("!")
				|| tokenText.equals(":") || tokenText.equals(";");
	}

}
