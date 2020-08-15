package com.argeworld.robotics.mokarobo;

public class Rhythm
{
    public enum NoteType
    {
        DON,
        KAT,
        BIGDON,
        BIGKAT
    }

    public NoteType noteType;
    public int startTime;

    public Rhythm(int startTime, int type)
    {
        this.startTime = startTime;

        if (type == 0)
        {
            this.noteType = NoteType.DON;
        }
        else if (type == 8)
        {
            this.noteType = NoteType.KAT;
        }
        else if (type == 4)
        {
            this.noteType = NoteType.BIGDON;
        }
        else if (type == 12)
        {
            this.noteType = NoteType.BIGKAT;
        }
    }
}
