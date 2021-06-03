docker manifest create pendl2/openjdk:11--amend pendl2/openjdk:11-slim --amend pendl2/openjdk:11-slim-armv7
docker manifest annotate --arch arm --os linux pendl2/openjdk:11 pendl2/openjdk:11-slim-armv7
docker manifest inspect pendl2/openjdk:11
docker manifest push pendl2/openjdk:11

