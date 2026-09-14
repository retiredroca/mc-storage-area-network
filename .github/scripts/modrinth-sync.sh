#!/usr/bin/env bash
#
# Publish a Modrinth version for one project: the primary jar plus any additional
# files (e.g. a bundle), then archive every superseded version so only the current
# one stays listed.
#
# Modrinth versions are effectively immutable, so an update is a new version rather
# than a mutated one; the previous version is archived afterwards. The new version id
# is written back to release-state.properties under <state_key>.
#
# Usage: modrinth-sync.sh <projectIdOrSlug> <stateKey> <versionNumber> <versionName> <primaryFile> [<extraFile>...]
#
set -euo pipefail

: "${MODRINTH_TOKEN:?MODRINTH_TOKEN is required}"

project="${1:?project id or slug required}"
state_key="${2:?state key required}"
version_number="${3:?version number required}"
version_name="${4:?version name required}"
primary="${5:?primary file required}"
shift 5
extras=("$@")

api='https://api.modrinth.com/v2'
state_file='release-state.properties'
previous="$(grep -E "^${state_key}=" "$state_file" | head -1 | cut -d= -f2 || true)"

data="$(jq -nc \
  --arg project_id "$project" \
  --arg name "$version_name" \
  --arg version_number "$version_number" \
  '{project_id: $project_id, name: $name, version_number: $version_number,
    version_type: "release", status: "listed", featured: false,
    loaders: ["fabric", "neoforge"], game_versions: ["1.21.1"], dependencies: []}')"

echo "modrinth: creating version ${version_number} on ${project}"
args=(-fsS -X POST "${api}/version" -H "Authorization: ${MODRINTH_TOKEN}"
      -F "data=${data};type=application/json"
      -F "file=@${primary}")
for extra in "${extras[@]}"; do
  args+=(-F "file=@${extra}")
done
response="$(curl "${args[@]}")"

version_id="$(echo "$response" | jq -r '.id // empty')"
if [ -z "$version_id" ]; then
  echo "modrinth: could not read the new version id from the response" >&2
  echo "$response" >&2
  exit 1
fi
echo "modrinth: created version ${version_id}"

sed -i -E "s|^${state_key}=.*|${state_key}=${version_id}|" "$state_file"
if ! grep -qE "^${state_key}=" "$state_file"; then
  echo "${state_key}=${version_id}" >> "$state_file"
fi

if [ -n "$previous" ] && [ "$previous" != "$version_id" ]; then
  echo "modrinth: archiving superseded version ${previous}"
  curl -fsS -X PATCH "${api}/version/${previous}" \
    -H "Authorization: ${MODRINTH_TOKEN}" \
    -H 'Content-Type: application/json' \
    -d '{"requested_status":"archived"}' || echo "modrinth: could not archive ${previous}" >&2
fi
