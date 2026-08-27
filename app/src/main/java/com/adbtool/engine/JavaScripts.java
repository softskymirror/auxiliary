/*
 *
 * MIT License
 *
 * Copyright (c) 2017 朱辉 https://blog.yeetor.com
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */

package com.adbtool.engine;

import com.adbtool.engine.js.Functions;
import com.adbtool.util.Constant;
import org.apache.log4j.Logger;

import javax.script.*;
import java.io.*;

/**
 * JavaScripts - JavaScript engine manager using standard javax.script API.
 * Loads and executes init.js scripts for device automation.
 */
public class JavaScripts {

    private static final Logger logger = Logger.getLogger(JavaScripts.class);

    /**
     * Load and execute the init.js script using the default JavaScript engine.
     */
    public static void test() {
        ScriptEngineManager manager = new ScriptEngineManager();
        ScriptEngine engine = manager.getEngineByName("JavaScript");

        if (engine == null) {
            logger.warn("No JavaScript engine available. Nashorn was removed in JDK 15+. " +
                    "Consider adding a standalone Nashorn or GraalJS dependency if JS scripting is needed.");
            return;
        }

        File initJs = Constant.getResourceFile("init.js");
        if (initJs == null || !initJs.exists()) {
            logger.warn("init.js not found at: " + (initJs != null ? initJs.getAbsolutePath() : "null"));
            return;
        }

        try (Reader fReader = new InputStreamReader(new FileInputStream(initJs))) {
            engine.eval(fReader);
        } catch (ScriptException e) {
            logger.error("Script execution error in init.js", e);
        } catch (FileNotFoundException e) {
            logger.error("init.js file not found: " + initJs.getAbsolutePath(), e);
        } catch (IOException e) {
            logger.error("IO error reading init.js", e);
        }
    }

    public void show() {
        System.out.println("...");
    }
}
