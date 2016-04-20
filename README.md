# Boomerang
A solution to the 'Boomerang Tournament' problem from round 1 of the [2016 Facebook Hacker Cup](https://www.facebook.com/hackercup/problem/1424196571244550/). 

This is a nice example of the dynamic programming technique. The program runs in under 3 seconds on the 250 cases I downloaded from Facebook.

A couple of outstanding questions I have:
* is it possible to tweak the approach so as to be able to solve tournaments where the number of players is not a power of 2?
* can the subproblem solutions be stored in a compact array format rather than a map?
* is there any optimisation that could make analysing a 32-player torunament feasible?
