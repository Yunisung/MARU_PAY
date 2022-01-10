#!/bin/sh 
kill -9 `cat < apio.pid`
rm apio.pid
