const express = require("express");
const crypto = require("crypto");

const app = express();

app.use(express.json({
    limit: "16kb"
}));

const PORT = process.env.PORT || 3000;

// Maximum lifetime of an advertisement.
const MASS_LIFETIME_MS =
    25 * 60 * 1000;

// Delete a mass if it has continuously reported
// zero members for 10 minutes.
const EMPTY_TIMEOUT_MS =
    10 * 60 * 1000;

// world -> advertisement
const masses = new Map();

app.get("/", (req, res) => {
    res.json({
        service: "WeCorpCC Corp Mass Lobby",
        status: "online"
    });
});

/*
 * Return public lobby advertisements.
 *
 * ownerToken is deliberately NOT returned.
 */
app.get("/masses", (req, res) => {
    cleanupMasses();

    const result =
        Array.from(masses.values())
            .map(mass => ({
                world: mass.world,
                members: mass.members,
                rule: mass.rule,
                startedAt: mass.startedAt,
                updatedAt: mass.updatedAt,
                emptySince: mass.emptySince
            }))
            .sort((a, b) =>
                b.members - a.members
            );

    res.json(result);
});

/*
 * Create a new mass advertisement.
 */
app.post("/masses", (req, res) => {
    cleanupMasses();

    const world =
        Number(req.body.world);

    const members =
        Number(req.body.members);

    const rule =
        String(
            req.body.rule || ""
        ).toUpperCase();

    if (
        !Number.isInteger(world) ||
        world <= 0
    ) {
        return res.status(400).json({
            error: "Invalid world"
        });
    }

    if (
        !Number.isInteger(members) ||
        members < 0
    ) {
        return res.status(400).json({
            error: "Invalid member count"
        });
    }

    if (
        rule !== "FFA" &&
        rule !== "SPLIT"
    ) {
        return res.status(400).json({
            error: "Rule must be FFA or SPLIT"
        });
    }

    const existing =
        masses.get(world);

    /*
     * Only one public advertisement per world.
     */
    if (existing) {
        return res.status(409).json({
            error:
                `W${world} is already advertised as ${existing.rule}`
        });
    }

    const now =
        Date.now();

    /*
     * Private ownership token.
     *
     * The advertiser needs this token to heartbeat
     * or remove its own advertisement.
     */
    const ownerToken =
        crypto.randomUUID();

    const mass = {
        world,
        members,
        rule,

        startedAt: now,
        updatedAt: now,

        emptySince:
            members === 0
                ? now
                : null,

        ownerToken
    };

    masses.set(
        world,
        mass
    );

    res.status(201).json({
        world: mass.world,
        members: mass.members,
        rule: mass.rule,
        startedAt: mass.startedAt,
        updatedAt: mass.updatedAt,
        emptySince: mass.emptySince,
        ownerToken
    });
});

/*
 * Heartbeat/update an advertisement.
 *
 * The advertising RuneLite client periodically sends
 * its current visible Corp member count.
 */
app.put("/masses/:world", (req, res) => {
    cleanupMasses();

    const world =
        Number(req.params.world);

    const members =
        Number(req.body.members);

    const ownerToken =
        String(
            req.body.ownerToken || ""
        );

    if (
        !Number.isInteger(world) ||
        world <= 0
    ) {
        return res.status(400).json({
            error: "Invalid world"
        });
    }

    if (
        !Number.isInteger(members) ||
        members < 0
    ) {
        return res.status(400).json({
            error: "Invalid member count"
        });
    }

    const mass =
        masses.get(world);

    if (!mass) {
        return res.status(404).json({
            error: "Mass advertisement not found"
        });
    }

    if (
        !ownerToken ||
        ownerToken !== mass.ownerToken
    ) {
        return res.status(403).json({
            error: "Invalid advertisement token"
        });
    }

    const now =
        Date.now();

    mass.members =
        members;

    mass.updatedAt =
        now;

    if (members > 0) {
        /*
         * Someone returned before the 10-minute
         * empty timeout, so reset the timer.
         */
        mass.emptySince =
            null;
    }
    else if (mass.emptySince == null) {
        mass.emptySince =
            now;
    }

    masses.set(
        world,
        mass
    );

    res.json({
        world: mass.world,
        members: mass.members,
        rule: mass.rule,
        startedAt: mass.startedAt,
        updatedAt: mass.updatedAt,
        emptySince: mass.emptySince
    });
});

/*
 * Remove the advertiser's own mass.
 */
app.delete("/masses/:world", (req, res) => {
    cleanupMasses();

    const world =
        Number(req.params.world);

    const ownerToken =
        String(
            req.body?.ownerToken || ""
        );

    const mass =
        masses.get(world);

    if (!mass) {
        return res.status(404).json({
            error: "Mass advertisement not found"
        });
    }

    if (
        !ownerToken ||
        ownerToken !== mass.ownerToken
    ) {
        return res.status(403).json({
            error: "Invalid advertisement token"
        });
    }

    masses.delete(world);

    res.json({
        removed: true,
        world
    });
});

/*
 * Remove expired advertisements.
 */
function cleanupMasses() {
    const now =
        Date.now();

    for (
        const [world, mass]
        of masses
        ) {
        const age =
            now - mass.startedAt;

        if (
            age >=
            MASS_LIFETIME_MS
        ) {
            masses.delete(world);
            continue;
        }

        if (
            mass.emptySince != null &&
            now - mass.emptySince >=
            EMPTY_TIMEOUT_MS
        ) {
            masses.delete(world);
        }
    }
}

/*
 * Server-side cleanup every 5 seconds.
 */
setInterval(
    cleanupMasses,
    5000
);

app.listen(PORT, () => {
    console.log(
        `WeCorpCC Lobby running on port ${PORT}`
    );
});