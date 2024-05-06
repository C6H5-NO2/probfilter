# Setup VeriFx
0. `cd` project root.
1. Download to project root the [Z3](https://github.com/Z3Prover/z3/releases/latest) release that matches your work environment.
2. Extract the library files to correct locations.
   ```sh
   Z3_NAME=<name without extension>
   DL_EXT=<dll || dylib || so>
   unzip -q "./$Z3_NAME.zip"
   mv "./$Z3_NAME/bin/libz3.$DL_EXT" "./libz3.$DL_EXT"
   mv "./$Z3_NAME/bin/libz3java.$DL_EXT" "./libz3java.$DL_EXT"
   mkdir -p "./probfilter-verifx/lib/"
   mv "./$Z3_NAME/bin/com.microsoft.z3.jar" "./probfilter-verifx/lib/com.microsoft.z3.jar"
   rm -r "./$Z3_NAME/"
   rm "./$Z3_NAME.zip"
   ```
3. Create a symbolic link due to the hard-coding of scan path in VeriFx.
   ```sh
   # on linux
   mkdir -p "./src/main"
   ln -s "$PWD/probfilter-verifx/src/main/verifx" "./src/main/verifx"
   ```
   ```bat
   rem on windows
   mkdir ".\src\main"
   mklink /d ".\src\main\verifx" "%CD%\probfilter-verifx\src\main\verifx"
   ```
4. Exclude these files from git.
   ```sh
   GIT_EXCL="./.git/info/exclude"
   echo "libz3.$DL_EXT" >> $GIT_EXCL
   echo "libz3java.$DL_EXT" >> $GIT_EXCL
   echo "probfilter-verifx/lib/com.microsoft.z3.jar" >> $GIT_EXCL
   echo "src/main/verifx" >> $GIT_EXCL
   ```
5. Uncomment `verifx` related lines in `build.sbt` and then `reload`.
