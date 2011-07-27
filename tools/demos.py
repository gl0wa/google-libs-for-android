#!/usr/bin/env python
#
# Repeats a command for each demo and reports the status.
# The command is executed from the root directory of the demo.
#
# Example:
# python demos.py "ant install"
# python demos.py "android update project -p ."
#

import os
import sys

def demos(command):
  tools_directory, script = os.path.split(os.path.abspath(__file__))
  root_directory, tools = os.path.split(tools_directory)
  demos_directory = os.path.join(root_directory, 'demos')
  demos = os.listdir(demos_directory)
  demos = list(filter(lambda x: not x.startswith('.'), demos))
  results = [0] * len(demos)
  for index in range(len(demos)):
    demo = demos[index]
    os.chdir(os.path.join(demos_directory, demo))
    results[index] = os.system(command)
  for index in range(len(demos)):
    demo = demos[index]
    result = 'ok' if results[index] == 0 else 'failed'
    print (demo, result)

def main():
  try:
    script, command = sys.argv
    demos(command);
  except ValueError:
    print('Usage: python demos.py <command>')
    print()
    print('Examples:')
    print('  python demos.py "ant install"')
    print('  python demos.py "android update project -p ."')

if __name__ == '__main__':
    main()

