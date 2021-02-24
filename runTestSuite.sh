#!/bin/bash

MODES=('cc' 'sc')
IMPLS=(3 8)
RUNS=3

ITERATIONS=$(( $RUNS * ${#IMPLS[@]} * ${#MODES[@]}))
IT=0

for modes_index in ${!MODES[@]}; do	
	echo
	echo "#########"
	echo "#Running "${MODES[modes_index]}
	echo "#########"
	echo 

	for impls_index in ${!IMPLS[@]}; do
		for (( c=1; c <= $RUNS; c++ )) do
			IT=$(($IT + 1))

			echo
			echo "#########"
			echo "#ITERATION "$IT"/"$ITERATIONS
			echo "#########"
			echo 
		
			make ${MODES[modes_index]} QUEUE=${IMPLS[impls_index]}
		done
	done
done
