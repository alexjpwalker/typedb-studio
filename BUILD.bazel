#
# GRAKN.AI - THE KNOWLEDGE GRAPH
# Copyright (C) 2019 Grakn Labs Ltd
#
# This program is free software: you can redistribute it and/or modify
# it under the terms of the GNU Affero General Public License as
# published by the Free Software Foundation, either version 3 of the
# License, or (at your option) any later version.
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU Affero General Public License for more details.
#
# You should have received a copy of the GNU Affero General Public License
# along with this program.  If not, see <https://www.gnu.org/licenses/>.
#

# NOTE: this file needs to be called `BUILD.bzl` to avoid conflicts with `build/` created by `yarn run build`

load("@graknlabs_bazel_distribution//brew:rules.bzl", "deploy_brew")
load("@graknlabs_dependencies//distribution:deployment.bzl", "deployment")
load("@graknlabs_bazel_distribution//artifact:rules.bzl", "artifact_extractor")
load("@graknlabs_common//test:rules.bzl", "native_grakn_artifact")
load("@graknlabs_dependencies//tool/release:rules.bzl", "release_validate_deps")

deploy_brew(
    name = "deploy-brew",
    type = "cask",
    snapshot = deployment['brew.snapshot'],
    release = deployment['brew.release'],
    formula = "//config/brew:grakn-workbase.rb",
)

native_grakn_artifact(
    name = "native-grakn-artifact",
    mac_artifact = "@graknlabs_grakn_core_artifact_mac//file",
    linux_artifact = "@graknlabs_grakn_core_artifact_linux//file",
    windows_artifact = "@graknlabs_grakn_core_artifact_windows//file",
    output = "grakn-core-server-native.tar.gz",
    visibility = ["//test:__subpackages__"],
)

artifact_extractor(
    name = "grakn-extractor",
    artifact = ":native-grakn-artifact",
)


release_validate_deps(
    name = "release-validate-deps",
    refs = "@graknlabs_workbase_workspace_refs//:refs.json",
    tagged_deps = [
        "graknlabs_grakn_core",
        "graknlabs_client_nodejs",
    ],
    tags = ["manual"]  # avoids running this target when executing bazel test //...
)