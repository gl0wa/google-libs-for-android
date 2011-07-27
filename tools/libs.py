#!/usr/bin/env python
#
# Updates the JARs in the demo libs/ directories
# with the current build output found in bin/.
#

import os
import shutil
import sys

def listdir(path):
  """List files at the specified path excluding hidden files"""
  return filter(lambda x: not x.startswith('.'), os.listdir(path))

def libs():
  tools = os.path.split(os.path.abspath(__file__))[0]
  root = os.path.split(tools)[0]
  bin = os.path.join(root, 'bin')
  demos = os.path.join(root, 'demos')
  directories = [os.path.join(demos, demo, 'libs') for demo in listdir(demos)]
  directories.append(os.path.join(root, 'tests', 'libs'))
  directories.sort()
  for directory in directories:
    for lib in listdir(directory):
      src = os.path.join(bin, lib);
      dst = os.path.join(directory, lib)
      if os.path.exists(src):
        shutil.copyfile(src, dst)
      else:
        print("WARNING:", lib, "does not exist")

def main():
  libs();

if __name__ == "__main__":
    main()

