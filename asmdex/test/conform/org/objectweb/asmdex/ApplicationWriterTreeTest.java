/* Software Name : AsmDex
 * Version : 1.0
 *
 * Copyright © 2012 France Télécom
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. Neither the name of the copyright holders nor the names of its
 *    contributors may be used to endorse or promote products derived from
 *    this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF
 * THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.objectweb.asmdex;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;

import org.junit.AfterClass;
import org.junit.Test;
import org.objectweb.asmdex.ApplicationReader;
import org.objectweb.asmdex.ApplicationWriter;
import org.objectweb.asmdex.tree.ApplicationNode;

/**
 * Tests the Application Writer using the Tree API.
 * This is done by comparing the output of the Writer, fed by the Reader, and the output of Baksmali.
 * 
 * @author Julien Névo
 */
public class ApplicationWriterTreeTest {

	/**
	 * After the class has been tested, we remove the temporary folder.
	 */
	@AfterClass
	public static void testAfter() {
		TestUtil.removeTemporaryFolder();
	}

	
	/**
	 * Test application writer.
	 */
	@Test
	public void testApplicationWriter() {
		// This constructor does nothing.
		new ApplicationWriter();
	}

	/**
	 * Tests if the generation to byte array is correct.
	 * For each dex file contained in the test/case folder :
	 *   - Runs Baksmali to create all the .smali file in the baksmali temporary folder.
	 *   - Runs Baksmali to create the files generated by the writer, in a second temporary folder.
	 *   - Compares the Maps information of both dex files. They should have the same
	 *     number of elements.
	 *   - Compares each .smali files :
	 *     Allows some dex files to skip some debug information : line number can be
	 *     incorrect in some rare and not useful cases. 
	 */
	@Test
	public void testToByteArray() throws IOException {
		boolean result;
		
		// Looks for all the dex files to test.
		File testCaseFolder;
		testCaseFolder = new File(TestUtil.PATH_FOLDER_TESTCASE + TestUtil.FULL_TEST_SUBFOLDER);
		result = testGenerationToByteArray(testCaseFolder, false);
		
		testCaseFolder = new File(TestUtil.PATH_FOLDER_TESTCASE + TestUtil.SKIP_LINE_NUMBERS_TEST_SUBFOLDER);
		result &= testGenerationToByteArray(testCaseFolder, true);

		assertTrue("Generated dex files aren't equal to the ones from dx.", result);
	}
	
	

	
	/**
	 * Tests if the generation to byte array is correct.
	 * For each dex file contained in the test/case folder :
	 *   - Runs Baksmali to create all the .smali file in the baksmali temporary folder.
	 *   - Runs Baksmali to create the files generated by the writer, in a second temporary folder.
	 *   - Compares the Maps information of both dex files. They should have the same
	 *     number of elements.
	 *   - Compares each .smali files :
	 *     Allows some dex files to skip some debug information : the line number can be
	 *     incorrect in some rare and not useful cases.
	 * @param skipLineNumbers true to skip the debug information about line numbers.
	 * @throws IOException 
	 */
	private boolean testGenerationToByteArray(File folder, boolean skipLineNumbers) throws IOException {
		boolean areFolderIdentical = true;
		
		for (File dexFile : folder.listFiles()) {
			String dexFileName = dexFile.getName(); 
			if (dexFileName.toLowerCase().endsWith(".dex")) {
				String fullDexFileName = folder.getPath() + "/" + dexFileName;
				
				TestUtil.removeTemporaryFolder();
				
				// Executes Baksmali to disassemble the current dex file.
				TestUtil.baksmali(new String[] { fullDexFileName,
						"-o" + TestUtil.TEMP_FOLDER_EXPECTED});
				
				// Uses the Tree API with the Reader and Writer to generate our own dex file from the current dex file.
				ApplicationReader ar = new ApplicationReader(fullDexFileName);
				ApplicationNode an = new ApplicationNode();
				ar.accept(an, 0);
				
				ApplicationWriter aw = new ApplicationWriter();
				an.accept(aw);
				
				byte[] generatedDexFile = aw.toByteArray();
				
				String fullGeneratedDexFileName = TestUtil.TEMP_FOLDER_ROOT + TestUtil.FILENAME_GENERATED_DEX;
				File createdDexFile = TestUtil.createFileFromByteArray(generatedDexFile, fullGeneratedDexFileName);
				
				// Tests the maps of both the original dex file and the generated one.
				assertTrue("Unequal Map between " + dexFileName + " and the generated file.", TestUtil.testMapDexFiles(createdDexFile, dexFile));
				
				// Executes Baksmali once again to disassemble our generated dex file.
				TestUtil.baksmali(new String[] { fullGeneratedDexFileName,
						"-o" + TestUtil.TEMP_FOLDER_GENERATED});
				
				// Compare the folders and the .smali files inside.
				areFolderIdentical = TestUtil.testSmaliFoldersEquality(TestUtil.TEMP_FOLDER_GENERATED,
						TestUtil.TEMP_FOLDER_EXPECTED, skipLineNumbers);
				assertTrue("Generated .smali files differ.", areFolderIdentical);
				TestUtil.removeTemporaryFolder();
			}
		}
		
		return areFolderIdentical;
	}
}
