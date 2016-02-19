#!/bin/bash
for i in $(find -name '*.java'); do
	echo "Lips processing: " $i
	lips < $i | astyle > output/$(basename $i)
	
done
