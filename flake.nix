{
  description = "A typed lisp with 2LTT for macros";

  inputs = {
    nixpkgs.url = "github:NixOS/nixpkgs/nixos-unstable";
    flake-utils.url = "github:numtide/flake-utils";
  };

  outputs = { self, nixpkgs, flake-utils, ... }:
    flake-utils.lib.eachDefaultSystem (system:
      let
        pkgs = import nixpkgs { inherit system; };
      in
      {
        devShells.default = pkgs.mkShell {
          buildInputs = [
            pkgs.gradle
            pkgs.openjdk21
            pkgs.kotlin
            pkgs.kotlin-language-server
          ];

          shellHook = ''
            export GRADLE_USER_HOME="$PWD/.gradle"
          '';
        };
      });
}

