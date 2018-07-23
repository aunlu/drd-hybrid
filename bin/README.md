DRD - Data Race Detector for Java programs


  What is it?
  -------------

  Data Race Detector (DRD) is tool for dynamic detection of data races in Java programs

  DRD is developed by Devexperts LLC (http://devexperts.com).

  Documentation is available at http://code.devexperts.com/display/DRD/.

  Copyright © 2002-2018 Devexperts LLC. Licensed under the GNU General Public License, version 3.0.
  Included Software

   DRD is implemented as java agent, i.e. it's executed in same JVM with target application.
  It dynamically tracks various events in program and finds apparent and potential data races during its execution.
  Race reports and other information are logged into log-files.
  For every detected race additional information is printed (stack traces, race target, etc.) to help to detect and fix race in source code.

  Using DRD
  -------------

  The profiled application should be run with "-javaagent:drd_agent.jar" JVM argument.
  Do not rename agent file "drd_agent.jar"!!!


  The Latest Version
  --------------------

  The latest version can be found at <http://code.devexperts.com/display/DRD/>
  Current status: release 0.3, 31.10.2012

  Documentation
  ---------------

  The documentation can be found at <http://code.devexperts.com/display/DRD/>


  Feedback
  ----------

  Feel free to submit feature requests and bug reports at <drd-support@devexperts.com>.


  Licensing
  -----------


  This software is licensed under the terms found in the file named "LICENSE".

  The copying permission statement should come right after the copyright notices. For a one-file program, the statement (for the GPL) should look like this:

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.

  ----------

  Thank you for using DRD.