package dev.welbyseely.emu.dbms.commands.query;

public record Aggregate(AggregateType type, String column) {}
