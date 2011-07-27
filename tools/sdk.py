#!/usr/bin/env python
#
# Builds archives for SDK releases.
#
# Usage:
# python tools/sdk.py <version>
#

import os
import shutil
import sys
import tempfile

def execute(command):
  status = os.system(command)
  if status != 0:
    raise Exception('unexpected result: %d' % status)

def sdk(version, output):
  # Find the location of this script
  tools, script = os.path.split(os.path.abspath(__file__))

  # Find the location of the working copy of the project
  workspace, tools = os.path.split(tools)

  # Create a root directory for the SDK
  directory = 'libs-for-android-%s' % version
  root = os.path.join(output, directory)

  # Export a clean copy of the source code
  execute('svn export http://libs-for-android.googlecode.com/svn/trunk/ %s' % root)

  # Build a clean copy of the JARs
  shutil.copy(os.path.join(workspace, 'local.properties'), os.path.join(root, 'local.properties'))
  execute('ant -f %s all javadoc' % os.path.join(root, 'build.xml'))
  shutil.rmtree(os.path.join(root, 'bin', 'classes'))
  os.remove(os.path.join(root, 'local.properties'))

  # Prepare (or remove) the demo files
  demos = os.path.join(root, 'demos')
  include = ['atom', 'rss', 'jamendo']
  for demo in os.listdir(demos):
    if demo in include:
      os.mkdir(os.path.join(demos, demo, 'bin'))
      os.mkdir(os.path.join(demos, demo, 'gen'))
    else:
      shutil.rmtree(os.path.join(demos, demo))

  # Remove the tests
  shutil.rmtree(os.path.join(root, 'tests'))

  # Remove this script from the output
  os.remove(os.path.join(root, 'tools', 'sdk.py'))  

  # Create compressed archives in multiple formats
  os.chdir(output)
  execute('tar czf libs-for-android-%s.tar.gz %s' % (version, directory))
  execute('zip -r libs-for-android-%s.zip %s' % (version, directory))

def main():
  script, version = sys.argv
  temp = tempfile.mkdtemp()
  sdk(version, output=temp);
  print('SDK archives created in', temp)

if __name__ == "__main__":
    main()

