/**
 * Copyright © 2008-2019, Province of British Columbia
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ca.bc.gov.ols.geocoder.dra;

import java.text.Normalizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ca.bc.gov.ols.geocoder.lexer.CleanRule;
import ca.bc.gov.ols.geocoder.lexer.JoinRule;
import ca.bc.gov.ols.geocoder.lexer.LexicalRules;

/**
 * Lexical restructuring rules for DRA
 * 
 * @author chodgson
 * 
 */

public class DraLexicalRules extends LexicalRules
{
	public static final String RE_WORD = "[^0-9]+|[\\w]{9,}";
	
	public static final String RE_AND = "AND";
	
	public static final String RE_NUMBER = "\\d{1,8}";
	
	public static final String RE_NUMBER_WITH_SUFFIX = "\\d{1,8}[a-zA-Z]";
	
	public static final String RE_NUMBER_WITH_OPTIONAL_SUFFIX = "\\d{1,8}([a-zA-Z])?";
	
	public static final String RE_ORDINAL = "(?i)(ST|TH|RD|ND|E|ER|RE|EME|ERE|IEME|IERE)";
	
	// unit numbers can be a single letter,
	// or an optional letter followed by some numbers followed by an optional letter
	public static final String RE_UNIT_NUMBER = "[a-zA-Z0-9]?\\d{0,8}((?<=\\d)[a-zA-Z])?";
	
	public static final String RE_DIRECTIONAL = "N|NW|NE|S|SE|SW|E|W";
	
	public static final String RE_PROVINCE = "BC|AB|YT|SK|MB|ON|QC|NB|NS|NL|NT|NU|PE";
	
	public static final String RE_SUFFIX = "[A-Z]|1/2";
	
	public static final String POSTAL_ADDRESS_ELEMENT = "/PJ";
	
	public static final String FRONT_GATE = "/FG";

	public static final String OCCUPANT_SEPARATOR = "/OS";

	private Pattern[] postalPatterns;
	
	private static CleanRule[] cleanRules = new CleanRule[] {
			// remove periods and apostrophes between letters, squish letters together
			new CleanRule("(?<=[a-zA-Z]|^)[.'](?=[a-zA-Z]|$)", ""),
			// remove diacritical marks
			new CleanRule("\\p{InCombiningDiacriticalMarks}+", ""),
			// replace ligatures with individual characters
			// (note this is only a small subset of ligatures based on experience)
			new CleanRule("æ", "ae"),
			new CleanRule("Æ", "AE"),
			new CleanRule("Œ", "OE"),
			new CleanRule("œ", "oe"),
			new CleanRule("½", " 1/2"),
			// change & into "and"
			new CleanRule("&", " and "),
			// change -- into "/FG"
			new CleanRule("(?<=[^-]|^)--(?=[^-]|$)", " " + FRONT_GATE + " "),
			// change ** into "/OS"
			new CleanRule("(?<=[^\\*]|^)\\*\\*(?=[^\\*]|$)", " " + OCCUPANT_SEPARATOR + " "),
			// replace invalid characters with spaces (including apostrophes, periods, dashes)
			new CleanRule("[^a-zA-Z0-9/]", " "),
			// reduce all whitespace to single spaces
			new CleanRule("\\s+", " ")
	};
	
	public DraLexicalRules() {
		
		setJoinRules(new JoinRule[] {
				JoinRule.createJoin("(?i)(B)RITISH", "(?i)(C)OLUMBIA"),
				JoinRule.createJoin("(?i)(C)OLUMBIE", "(?i)(B)RITANIQUE"),
				JoinRule.createJoin("(?i)(C)", "(?i)(B)")
		});
		
		// setup patterns for handling postal junk
		postalPatterns = new Pattern[] {
				// postal code eg: V9K 1X9
				Pattern.compile(
						"\\b[ABCEGHJ-NPRSTVXY][0-9][ABCEGHJ-NPRSTV-Z]\\s*[0-9][ABCEGHJ-NPRSTV-Z][0-9]\\b",
						Pattern.CASE_INSENSITIVE),
				// Postal Box eg: PO BOX ## STN ABC
				Pattern.compile("\\b(PO\\s*)?BOX\\s*[0-9]+\\s*(STN\\s+[^\\s]*)?\\b(?!.*\\/FG)",
						Pattern.CASE_INSENSITIVE),
				// Mailbag eg MAILBAG ##
				Pattern.compile("\\b(((MAIL)?BAG)|LCD)\\s*[0-9]+\\b(?!.*\\/FG)", Pattern.CASE_INSENSITIVE),
				// Rural Routes/Mail Route/SS eg: RR ##
				Pattern.compile("\\b(RR|MR|SS|RURAL ROUTE)\\s*[0-9]+\\s*(STN\\s+[^\\s]*)?\\b(?!.*\\/FG)",
						Pattern.CASE_INSENSITIVE),
				// General Delivery Station eg: GD STN ABC
				Pattern.compile("\\b(GD\\s*)?STN\\s+[^\\s]+\\b(?!.*\\/FG)", Pattern.CASE_INSENSITIVE),
				// General Delivery
				Pattern.compile("\\bGENERAL\\s+DELIVERY\\b", Pattern.CASE_INSENSITIVE)
		};
		
	}
	
	@Override
	public String cleanSentence(String sentence) {
		return clean(sentence);
	}
	
	public static String clean(String sentence) {
		sentence = Normalizer.normalize(sentence, Normalizer.Form.NFD);
		for(CleanRule rule : cleanRules) {
			sentence = rule.clean(sentence);
		}
		return sentence;
	}
	
	@Override
	public String runSpecialRules(String sentence) {
		// here we handle postal junk; we remove any and all postal-related junk, and
		// put a special "postal junk" flag on the end of the string where it is easy to find
		boolean foundPostalJunk = false;
		for(Pattern pp : postalPatterns) {
			Matcher matcher = pp.matcher(sentence);
			if(matcher.find()) {
				sentence = matcher.replaceAll("");
				foundPostalJunk = true;
			}
		}
		if(foundPostalJunk) {
			sentence += " " + POSTAL_ADDRESS_ELEMENT;
		}
		return sentence;
	}
}
